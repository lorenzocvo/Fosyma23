package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import debug.Debug;
import eu.su.mas.dedale.env.EntityCharacteristics;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.ReceiveTreasureTankerBehaviour;
import eu.su.mas.dedale.mas.agent.knowledge.AgentObservableElement;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.agents.dummies.DummyTankerAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;


import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import eu.su.mas.dedale.mas.agent.knowledge.AgentObservableElement;


/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * @author hc
 *
 */
public class SiloTankerBehaviour extends TickerBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	private List<String> list_agentNames;
	
	private Location start = null;
	
	private List<String> tresorloc = new ArrayList<>();
	
	private List<Integer> tresorvalue = new ArrayList<>();
	
	//on trie les 2 listes en fonction de la quantité croissante
	
	private List<String> talking = new ArrayList<>(); //ou talked to
	
	private List<Integer> cpt = new ArrayList<>();
	
	private Integer z = 0;
	
	//private List<String> arrived = new ArrayList<>(); //agents arrivés au rassemblement
	//en content de message d'arrivée nom,lockpicking,backpack/ emplacement des trésors/valeur des trésors

	private List<Couple<String,Integer>> agentscapacity = new ArrayList<>();
	//nomagent/backpack
	//triée par ordre croissant ou décroissant capa backpack
	
	private List<Couple<String, List<String>>> everypath = new ArrayList<>();
	// agent/path,récolte
	//mettre juste le path, les agents s'adaptent car ils ont la liste par ordre croissant
	//ceux qui n'ont pas de trésors changent de comportement
	
	private Boolean first = true;

	
	
	/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public SiloTankerBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,List<String> agentNames, List<String> tresloc, List<Integer> tresval, List<Couple<String,Integer>>agentcapa) {
		super(myagent,10);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		this.tresorloc = tresloc;
		this.tresorvalue = tresval;
		this.agentscapacity = agentcapa;
		//trier les agents en fonction de leur capacité
		/*
		List<Integer> ordre = new ArrayList<>();
		
		while(ordre.size()!=this.agentscapacity.size()) {
			Integer tmp = Integer.MAX_VALUE;
			Integer itmp =-1;
			for(int i=0;i<this.agentscapacity.size();i++) {
				
				if(this.agentscapacity.get(i).getRight()<tmp && !ordre.contains(i)) {
					
					itmp = i;
					tmp = this.agentscapacity.get(i).getRight();
					
				}
				
				
			}
			ordre.add(0, itmp);
			
		}
		
		List<Couple<String,Integer>> newagents = new ArrayList<>();
		
		
		for(int i=0;i<ordre.size();i++) {
			
			newagents.add(this.agentscapacity.get(ordre.get(i)));
			
			
		}
		System.out.println("capacity "+this.agentscapacity.toString());
		this.agentscapacity = newagents;
		
		
		
		//on attribue coffre par coffre (si le dernier agent peut ouvrir le dernier coffre on lui
		//donne, sinon on regarde l'avant dernier etc...
		//si aucun agent ne peut l'ouvrir alors on regarde qui peut y arriver en 2 coups etc...
		//s'il y a des agents qui n'ont pas de coffres attribués, soit ils se baladent de coffres
		//en coffres pour vérifier si personne n'a besoin d'aide, sinon ils bloquent un spot 
		//pour empecher le wumpus de venir
		
		
		//initialiser agentcapacity en fonction de l'ordre croissant
		
		System.out.println("capacity "+this.agentscapacity.toString());
		
		
		for(int i = 0;i<this.agentscapacity.size();i++) {
			
			List <String> tmp = new ArrayList<>();
			this.everypath.add(new Couple(this.agentscapacity.get(i).getLeft(),tmp));
		}
		
		
		
		for(int i=0;i<this.tresorloc.size();i++) {
			
			
			
			//mettre une variable pour while coffre non attribué
			int z = 1;
			
			
			
			//attention à ne pas tout attribuer au même agent
			while(z!=0) {
				for(int j = 0;j<this.agentscapacity.size();j++) {
					
					
					System.out.println(this.tresorvalue.get(i));
					System.out.println(this.agentscapacity.get(j).getRight());
					if(this.tresorvalue.get(i)/z<this.agentscapacity.get(j).getRight()) {
						
						//peut etre améliorer ça (ou pas)
						this.everypath.get(j).getRight().add(this.tresorloc.get(i));
						
						
						z = 0;
						break;
						
					}
					
					
				}
				
				if( z!=0) {
					
					z++;
				}
			}
			
			
		}
		
		
		//on fait le message spécial et on l'envoie
		
		
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("LAUNCH-RECOLTE");
		msg.setSender(this.myAgent.getAID());
		
		String contenu ="";
		
		for(int i =0; i<this.everypath.size();i++) {
			
			//nom de l'agent
			contenu = contenu + this.everypath.get(i).getLeft() + ",";
			
			
			for(int j=0;j<this.everypath.get(i).getRight().size();j++) {
				
				//chaque location de trésor allouée séparée par une virgule
				contenu = contenu + this.everypath.get(i).getRight().get(j) + ",";
				
				
			}
			
			contenu = contenu.substring(0, contenu.length()-1);
			contenu = contenu + "/";
			
			
		}
		
		msg.setContent(contenu);
		
		for (String agentName : this.list_agentNames) {
			
			if(!this.talking.contains(agentName)) {
				
				msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			}
		}
		
		((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		System.out.println(msg);
		
		//System.out.println(this.myAgent.getBehavioursCnt());
		*/
		
	}

	@Override
	public void onTick() {

		
		
		
		if(this.first) {
			
			this.first = false;
			
			List<Integer> ordre = new ArrayList<>();
			
			while(ordre.size()!=this.agentscapacity.size()) {
				Integer tmp = Integer.MAX_VALUE;
				Integer itmp =-1;
				
				
				for(int i=0;i<this.agentscapacity.size();i++) {
					
					if(this.agentscapacity.get(i).getRight()<tmp && !ordre.contains(i)) {
						
						itmp = i;
						tmp = this.agentscapacity.get(i).getRight();
						
					}
					
				}
				ordre.add(itmp);
				
			}
			
			List<Couple<String,Integer>> newagents = new ArrayList<>();
			
			
			for(int i=0;i<ordre.size();i++) {
				
				newagents.add(this.agentscapacity.get(ordre.get(i)));
				
				
			}
			System.out.println("capacity "+this.agentscapacity.toString());
			this.agentscapacity = newagents;
			
			
			
			//on attribue coffre par coffre (si le dernier agent peut ouvrir le dernier coffre on lui
			//donne, sinon on regarde l'avant dernier etc...
			//si aucun agent ne peut l'ouvrir alors on regarde qui peut y arriver en 2 coups etc...
			//s'il y a des agents qui n'ont pas de coffres attribués, soit ils se baladent de coffres
			//en coffres pour vérifier si personne n'a besoin d'aide, sinon ils bloquent un spot 
			//pour empecher le wumpus de venir
			
			
			//initialiser agentcapacity en fonction de l'ordre croissant
			
			System.out.println("capacity "+this.agentscapacity.toString());
			
			
			for(int i = 0;i<this.agentscapacity.size();i++) {
				
				List <String> tmp = new ArrayList<>();
				this.everypath.add(new Couple(this.agentscapacity.get(i).getLeft(),tmp));
			}
			
			
			
			for(int i=0;i<this.tresorloc.size();i++) {
				
				
				
				//mettre une variable pour while coffre non attribué
				int z = 1;
				
				
				
				//attention à ne pas tout attribuer au même agent
				while(z!=0) {
					for(int j = 0;j<this.agentscapacity.size();j++) {
						
						
						System.out.println(this.tresorvalue.get(i));
						System.out.println(this.agentscapacity.get(j).getRight());
						
						if(this.tresorvalue.get(i)/z<=this.agentscapacity.get(j).getRight()) {
							
							//peut etre améliorer ça (ou pas)
							this.everypath.get(j).getRight().add(this.tresorloc.get(i));
							
							
							z = 0;
							break;
							
						}
						
					}
					
					if( z!=0) {
						
						z++;
					}
				}
				
				
			}
			
			
			//on fait le message spécial et on l'envoie
			
			
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("LAUNCH-RECOLTE");
			msg.setSender(this.myAgent.getAID());
			
			String contenu ="";
			
			for(int i =0; i<this.everypath.size();i++) {
				
				//nom de l'agent
				contenu = contenu + this.everypath.get(i).getLeft() + ",";
				
				
				for(int j=0;j<this.everypath.get(i).getRight().size();j++) {
					
					//chaque location de trésor allouée séparée par une virgule
					contenu = contenu + this.everypath.get(i).getRight().get(j) + ",";
					
					
				}
				
				contenu = contenu.substring(0, contenu.length()-1);
				contenu = contenu + "/";
				
				
			}
			
			msg.setContent(contenu);
			
			for (String agentName : this.list_agentNames) {
				
				if(!this.talking.contains(agentName)) {
					
					msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
				}
			}
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			System.out.println(msg);
			
			
		}
		
		
		//((AbstractDedaleAgent)this.myAgent).loadEntityCaracteristics(STATE_READY, STATE_BLOCKED);
		
		//ReceiveTreasureTankerBehaviour.PROTOCOL_TANKER;
		
		
		
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		if(this.start==null) {
			this.start = myPosition;
		}
		
		
		
		if (myPosition!=null){
			//List of observable from the agent's current position
			//List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			/*
			try {
				this.myAgent.doWait(10);
			} catch (Exception e) {
				e.printStackTrace();
			}


			
			
				/*
			MessageTemplate template= 
					MessageTemplate.and(
							MessageTemplate.MatchProtocol(PROTOCOL_TANKER),
							MessageTemplate.MatchPerformative(ACLMessage.REQUEST)		
							);

			//I'm waiting for a message from a collector
			ACLMessage msg=this.myAgent.receive(template);
			//Couple<Observation,Integer> c=null;
			List<Couple<Observation, Integer>>c=null;
			if (msg!=null){
				//Debug.warning("Tanker agent - Message received: "+msg.toString());
				try {
					c=(List<Couple<Observation, Integer>>) msg.getContentObject();
				} catch (UnreadableException e) {
					Debug.error("Tanker receiving non Deserializable value");
					e.printStackTrace();
				}
				c.forEach(x -> {
					Integer i=this.ec.getBackPackCapacity(x.getLeft())-this.aoe.getBackPackUsedSpace((x.getLeft()));
					if (i>0){
						//	add the received value in the agentTanker backpack
						this.aoe.add2TreasureValue(x.getLeft(), x.getRight());
						//Debug.warning(c.getLeft()+" - There is now "+this.backPack.get(c.getLeft()) +" in the backPack");
					}else{
						this.aoe.add2TreasureValue(x.getLeft(), i);//the remaining is lost
					}
				});
				//Integer i=this.ec.getBackPackCapacity(c.getLeft())-this.aoe.getBackPackUsedSpace((c.getLeft()));
				
				ACLMessage resp=msg.createReply();
				resp.setPerformative(ACLMessage.AGREE);
				this.myAgent.send(resp);
			}else{
				block();
			}
				
			*/
		
			
			
			if(this.talking.size()>0) {
				
				for(int i=0;i<this.talking.size();i++) {
					
					this.cpt.set(i, this.cpt.get(i)+1);
					if(this.cpt.get(i)>10) {
						this.talking.remove(i);
						this.cpt.remove(i);
					}
					
				}
				
			}
			
			
			
			//this.myAgent.addBehaviour((new ReceiveTreasureTankerBehaviour(((AbstractDedaleAgent)this.myAgent), new AgentObservableElement(this.myAgent.getLocalName()),   )));
			//((AbstractDedaleAgent)this.myAgent).removeBehaviour(this);
			
			//ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			
			//((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
			
			
			
			

		}
	}
}
/*
	@Override
	public boolean done() {
		return finished;
	}

}*/





