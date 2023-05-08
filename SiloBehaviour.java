package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SiloTankerBehaviour;


import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


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
public class SiloBehaviour extends SimpleBehaviour {

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
	
	private List<String> talking = new ArrayList<>(); //ou talked to
	
	private List<Integer> cpt = new ArrayList<>();
	
	private Integer z = 0;
	
	private List<String> arrived = new ArrayList<>(); //agents arrivés au rassemblement
	//en content de message d'arrivée nom,lockpicking,backpack/ emplacement des trésors/valeur des trésors

	private Boolean first = true;
	
	List<Couple<String,Integer>>agentcapa = new ArrayList<>();
	
	/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public SiloBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,List<String> agentNames) {
		super(myagent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		
		
		
		
	}

	@Override
	public void action() {

		if(this.myMap==null) {
			this.myMap= new MapRepresentation();
			//this.myAgent.addBehaviour(new ShareMapBehaviour(this.myAgent,500,this.myMap,list_agentNames));
		}
		
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		if(this.start==null) {
			this.start = myPosition;
		}
		
		

		
		

		if (myPosition!=null){
			//List of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be too quick
			 */
			try {
				this.myAgent.doWait(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}


			
			MessageTemplate msgTemplate=MessageTemplate.MatchAll();
			ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
			System.out.println(msgReceived);
			
			
			
			if (msgReceived == null) {
			
				
				//on reprend la marche
				//on enlève ce behaviour
				if(this.z==1) {
					this.z=0;
					//this.move.set(0, true);
				}else {
					this.z=1;
				}
				
			}else {
				
				
				this.z = 0;
				
				
				if(msgReceived.getProtocol()=="SHARE-MAP-SILO" && !this.talking.contains(msgReceived.getSender().getLocalName())) {
					
					this.talking.add(msgReceived.getSender().getLocalName());
					this.cpt.add(0);
					
					SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
					try {
						sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
					} catch (UnreadableException e) {
							// TODO Auto-generated catch block
						e.printStackTrace();
					}
					this.myMap.mergeMap(sgreceived);
					
					
					
					
				}
				else if(msgReceived.getProtocol()=="ASK-ARRIVED") {
					
					ACLMessage msg = msgReceived.createReply();
					//this.arrived.add(msgReceived.getSender().getLocalName());
					if(this.first) {
						msg.setProtocol("FIRST-ARRIVED");
						this.first = false;
					}else {
						msg.setProtocol("ARRIVED");
					}
					msg.setSender(this.myAgent.getAID());
						
					((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
					
				}else if(msgReceived.getProtocol()=="FULL-MAP") {
					ACLMessage msg = msgReceived.createReply();
					msg.setProtocol("ARRIVED");
					msg.setSender(this.myAgent.getAID());
					
					SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
					try {
						sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
					} catch (UnreadableException e) {
							// TODO Auto-generated catch block
						e.printStackTrace();
					}
					this.myMap.mergeMap(sgreceived);
					
					((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
					
					
					
				}
				else if(msgReceived.getProtocol()=="INFORMATIONS") {
					
					if(!this.arrived.contains(msgReceived.getContent())) {
						
						
						List<String> tmp = new ArrayList<String>(Arrays.asList(msgReceived.getContent().split("/")));
						
						this.arrived.add(tmp.get(0));
						
						
						Boolean check = true;
						
						for(int i=0;i<this.agentcapa.size();i++) {
							
							if(this.agentcapa.get(i).getLeft().equals(tmp.get(0))) {
								
								check = false;
							}
							
							
						}
						
						if(check) {
							this.agentcapa.add(new Couple(tmp.get(0),Integer.parseInt(tmp.get(1))));
							System.out.println("agent capa"+this.agentcapa);
							List<String> tmploc = new ArrayList<String>(Arrays.asList(tmp.get(2).split(",")));
							List<String> tmpval = new ArrayList<String>(Arrays.asList(tmp.get(3).split(",")));
							
							for(int i =0;i<tmploc.size();i++) {
								
								if(!this.tresorloc.contains(tmploc.get(i))) {
									try {
										this.tresorloc.add(tmploc.get(i));
										this.tresorvalue.add(Integer.parseInt(tmpval.get(i)));
										
									}catch (Exception e) {
										
										System.out.println("tresor loc" + this.tresorloc);
										System.out.println("tresor val" + this.tresorvalue);
										e.printStackTrace();
									}
									
								}
								
							}
						}
						
						
						
						
					}
					
					
					
				}
				
				
			}
				
			
			//mettre capa à la place
			if(this.agentcapa.size()==this.list_agentNames.size()-1) {
				
				System.out.println("C4EST BON POUR VOUS");
				
				this.myAgent.addBehaviour(new SiloTankerBehaviour((AbstractDedaleAgent)this.myAgent, this.myMap, this.list_agentNames, this.tresorloc,this.tresorvalue, this.agentcapa));
				
				this.finished = true;
				((AbstractDedaleAgent)this.myAgent).removeBehaviour(this);
			
			}
			
			if(this.talking.size()>0) {
				
				for(int i=0;i<this.talking.size();i++) {
					
					this.cpt.set(i, this.cpt.get(i)+1);
					if(this.cpt.get(i)>30) {
						this.talking.remove(i);
						this.cpt.remove(i);
					}
					
				}
				
			}
			
			
			
			
			if(this.myAgent!=null) {
			
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setProtocol("SHARE-SILO");
				msg.setSender(this.myAgent.getAID());
				
				msg.setContent(this.start.getLocationId());
				//mettre quelque chose dans le content(liste trésors)? path?
				
				for (String agentName : this.list_agentNames) {
					
					
					System.out.println(this.talking);
					if(!this.talking.contains(agentName)) {
						System.out.println(agentName);
						msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
					}
				}
				
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
			
			}
			//rajouter du mouvement et le fait de l'ajouter à la connaissance de la map
			
			

		}
		//System.out.println("arrived :"+this.arrived.toString());
	}

	@Override
	public boolean done() {
		return finished;
	}

}
