package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.TalkingCoopBehaviour;


import jade.core.AID;
import jade.core.Agent;
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
public class RecolteBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	private List<String> list_agentNames;

	
	private List<String> path = new ArrayList<>();
	
	private List<String> talking = new ArrayList<>(); //ou talked to
	
	private List<Boolean> move = new ArrayList<>();;
	
	private Location silo;
	
	private List<Couple<Location, Couple<Integer, Integer>>> tresors = new ArrayList<>();
	
	private List<Location> tresortest = new ArrayList<>();
	
	private int tick = 0;
	
	//private Location objectif;
	
	//les agents envoyés pour attendre sur un trésor
	private Boolean recolte = true;
	
	//mettre variable mission?
	//mission donnée par le silo, il revient le voir après l'avoir accomplie
	
	
	private String fullpath;
	
	private List<String> objectif = new ArrayList<>();
	
/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public RecolteBehaviour(final Agent myagent, MapRepresentation myMap,List<String> agentNames, String everypath, Location siloloc) {
		super(myagent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		this.move.add(true);
		this.silo = siloloc;
		
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		this.fullpath = everypath;
		
		//traitement du fullpath
		//List<String> full = new ArrayList<String>(Arrays.asList(msgReceived2.getContent().split("/")));
		List<String> traitement = new ArrayList<String>(Arrays.asList(this.fullpath.split("/")));
		
		//System.out.println("traitement "+traitement.toString());
		for(int i =0;i<traitement.size();i++) {
			
			List<String> tmp = new ArrayList<String>(Arrays.asList(traitement.get(i).split(",")));
			
			//System.out.println("nom = "+tmp.get(0)+"/"+this.myAgent.getLocalName());
			//System.out.println(tmp.get(0)==this.myAgent.getLocalName());
			//System.out.println(StringUtils.difference(tmp.get(0), this.myAgent.getLocalName()));
			if(tmp.get(0).equals(this.myAgent.getLocalName())) {
				
				this.objectif = tmp;
				this.objectif.remove(0);
				break;
				
			}
			
			
			
		}
		
		//System.out.println(this.objectif);
		if(this.objectif.size()>0) {
			this.path = this.myMap.getShortestPath(myPosition.toString(), this.objectif.get(0));
		}else {
			//si le path est null, peut etre mettre une variable pour
			//faire le tour des coffres et aider les agents à les ouvrir si besoin
			this.path = null;
		}
		
	}

	@Override
	public void action() {
		
		//attention à ne pas couper en pleine communication
		this.tick++;
		
		//changer les déplacements (ajouter déplacement random si coffre, sinon attendre)
		//et attendre en cas de blocage
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		
		
		try {
			this.myAgent.doWait(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//optimiser : d'abord on regarde si on a reçu, puis on bouge, puis on envoie
		
		
		//autre behaviour de communication pour sortir de blocages complexes

		

		List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
		
		Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
		List<Location> nextto = new ArrayList<>();
		
		while(iter.hasNext()){
			
			Couple<Location, List<Couple<Observation, Integer>>> iter2 = iter.next();
			Location accessibleNode=iter2.getLeft();
			//System.out.println(iter2);
			
			//Location accessibleNode=iter.next().getLeft();
			boolean isNewNode=this.myMap.addNewNode(accessibleNode.getLocationId());
			
			
			//the node may exist, but not necessarily the edge
			if (myPosition.getLocationId()!=accessibleNode.getLocationId()) {
				this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
				nextto.add(accessibleNode);
				
				
			}
		}
		
		
		
		String nextNodeId;
		
		if(!myPosition.toString().equals(this.objectif.get(0))  ) {
			
			//if path.size()?
			
			if(this.path.size()==0) {
				
				this.path = this.myMap.getShortestPath(myPosition.toString(), this.objectif.get(0));
			}

			nextNodeId=this.path.get(0);
			if (nextNodeId==null){

				nextNodeId = iter.next().getLeft().getLocationId();
				//System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
			}
			
			
			
			Random r = null;
			int moveid;
			
			if(((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
				this.path.remove(0);
			}else {
				
				while( (nextto.size()>0) && !((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
					
					if(r!=null) {
						nextto.remove(r);
					}
					r = new Random();
					moveid = r.nextInt(nextto.size());
					nextNodeId = nextto.get(moveid).getLocationId();
				}
				myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
				this.path = this.myMap.getShortestPath(myPosition.toString(), this.objectif.get(0));
				
			}
			
			
			
			if(!this.recolte && this.path.size()==1) {
				
				System.out.println("J'aurais essayé");
				System.out.println(this.list_agentNames);
				System.out.println(this.list_agentNames.get(this.list_agentNames.size()-1));
				
				Boolean test = ((AbstractDedaleAgent)this.myAgent).emptyMyBackPack(this.list_agentNames.get(this.list_agentNames.size()-1));
				while(!test) {
					System.out.println(test);
					 test = ((AbstractDedaleAgent)this.myAgent).emptyMyBackPack(this.list_agentNames.get(this.list_agentNames.size()-1));
				}
				//System.out.println(((AbstractDedaleAgent)this.myAgent).emptyMyBackPack(this.list_agentNames.get(this.list_agentNames.size()-1))); 
					
				
				this.objectif.remove(0);
				this.recolte=true;
				//this.path = this.myMap.getShortestPath(myPosition.toString(), this.objectif.get(0));
			}
		
		
		}else {
			
			if(this.objectif.size()>0) {
				((AbstractDedaleAgent)this.myAgent).openLock(Observation.GOLD);
				((AbstractDedaleAgent)this.myAgent).pick();
				
				//si il ne reste plus de place dans le backpack, on y retourne
				if(((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace().get(0).getRight()!=0) {
					this.objectif.remove(0);
				}
				
				if(this.recolte) {
					this.objectif.add(0,this.silo.toString());
					this.recolte = false;
				}else {
					this.recolte = true;
				}
				
				
				
				if(this.path.size()<=0) {
					this.path = this.myMap.getShortestPath(myPosition.toString(), this.objectif.get(0));
				}
				
				
			}else {
				
				//on a terminé
				
				
			}
			
			
			
			
			
		}
		
		
		
		
		

		
	}
	

	@Override
	public boolean done() {
		return finished;
	}

}
