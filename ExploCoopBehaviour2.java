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
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.TalkingCoopBehaviour;


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
public class ExploCoopBehaviour2 extends SimpleBehaviour {

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
	
	private Location silo = null;
	 
	private List<String> tresorloc = new ArrayList<>();
	
	private List<Integer> tresorvalue = new ArrayList<>();
	
	private int tick = 0;
	
	private int z = 0;
	
	private List<Integer> cpt = new ArrayList<>();
	
/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public ExploCoopBehaviour2(final AbstractDedaleAgent myagent, MapRepresentation myMap,List<String> agentNames) {
		super(myagent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		this.move.add(true);
		
	}

	@Override
	public void action() {
		
		
		this.tick++;
		
		try {
			this.myAgent.doWait(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		if(this.myMap==null) {
			this.myMap= new MapRepresentation();
			//triche sinon le programme ne passe pas à l'étape suivante
			this.myMap.addNode("h40", MapAttribute.closed);
		}

		

		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		
		MessageTemplate msgTemplate=MessageTemplate.MatchAll();
		ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
		System.out.println(msgReceived);
		
		if (msgReceived == null) {
			/*
				if(this.z==1) {
					this.z=0;
					this.move.set(0, true);
				}else {
					this.z=1;
				}
			*/
		}else {
			this.move.set(0, false);
			this.z = 0;
			if(msgReceived.getProtocol() == "SHARE-TOPO" && !this.talking.contains(msgReceived.getSender().getLocalName())) {
				
				this.talking.add(msgReceived.getSender().getLocalName());
				this.cpt.add(0);
				
				
				if(msgReceived.getContent()!=null && this.silo == null) {
					this.silo = new gsLocation(msgReceived.getContent());
					this.myMap.addNode(this.silo.getLocationId(), MapAttribute.closed);
				}
				
				
				ACLMessage msg = msgReceived.createReply();
				msg.setProtocol("SHARE-MAP");
				msg.setSender(this.myAgent.getAID());
					
					
						
				SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
				try {					
					msg.setContentObject(sg);
				} catch (IOException e) {
					e.printStackTrace();
				}
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
					
				
			
			}else if(msgReceived.getProtocol()=="SHARE-MAP" && !this.talking.contains(msgReceived.getSender().getLocalName())) {
				
				SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
				try {
					sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
				} catch (UnreadableException e) {
						// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.myMap.mergeMap(sgreceived);
				this.talking.add(msgReceived.getSender().getLocalName());
				this.cpt.add(0);
				ACLMessage msg = msgReceived.createReply();
				msg.setProtocol("SHARE-MAP2");
				msg.setSender(this.myAgent.getAID());
					
					
						
				SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
				try {					
					msg.setContentObject(sg);
				} catch (IOException e) {
						e.printStackTrace();
				}
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
					
					
					
			}else if(msgReceived.getProtocol()=="SHARE-MAP2") {
					
					
				SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
				try {
					sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
				} catch (UnreadableException e) {
						// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.myMap.mergeMap(sgreceived);
					
				
				//on se sépare
				Random rand = new Random();
				try {
					Integer index = rand.nextInt(this.myMap.getOpenNodes().size());
					this.myMap.getShortestPath(myPosition.getLocationId(),this.myMap.getOpenNodes().get(index));
				}catch(Exception e) {
					
				}
					
			}else if(msgReceived.getProtocol()=="REGROUPEMENT"){
				
				//si on croise quelqu'un en regroupement on lui demande sa map (silo accessible de le protocole avec un split?)
				//mettre le silo en content de ce message(string donc peu couteux)
				this.talking.add(msgReceived.getSender().getLocalName());
				this.cpt.add(0);
				
				
				if(this.silo == null) {
					this.silo = new gsLocation(msgReceived.getContent());
					this.myMap.addNode(this.silo.getLocationId(), MapAttribute.closed);
				}
				
				
			}else if(msgReceived.getProtocol()=="FULL-MAP") {
				
				
				SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
				try {
					sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
				} catch (UnreadableException e) {
						// TODO Auto-generated catch block
					e.printStackTrace();
				}
				this.myMap.mergeMap(sgreceived);
				
				
			}else if(msgReceived.getProtocol()=="SHARE-SILO" && !this.talking.contains(msgReceived.getSender().getLocalName())) {
				
				this.talking.add(msgReceived.getSender().getLocalName());
				this.cpt.add(0);
				
				if(this.silo==null) {
					this.silo = new gsLocation(msgReceived.getContent());
					this.myMap.addNode(this.silo.getLocationId(), MapAttribute.closed);
				}
				
				
				ACLMessage msg = msgReceived.createReply();
				msg.setProtocol("SHARE-MAP-SILO");
				msg.setSender(this.myAgent.getAID());
					
					
						
				SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
				try {					
					msg.setContentObject(sg);
				} catch (IOException e) {
						e.printStackTrace();
				}
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
				
			}
			
		}
		
		

		if (myPosition!=null && this.move.get(0)){
			

			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

			//1) remove the current node from openlist and add it to closedNodes.
			this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);

			
			
			//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
			
			Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
			List<Location> nextto = new ArrayList<>();
			
			while(iter.hasNext()){
				
				Couple<Location, List<Couple<Observation, Integer>>> iter2 = iter.next();
				Location accessibleNode=iter2.getLeft();
				
				boolean isNewNode=this.myMap.addNewNode(accessibleNode.getLocationId());
			
				
				if(iter2.getRight().size()>0 && iter2.getRight().get(0).getLeft()==Observation.GOLD) {
					
					if(!this.tresorloc.contains(accessibleNode.getLocationId())) {
						
						this.tresorloc.add(accessibleNode.getLocationId());
						this.tresorvalue.add(iter2.getRight().get(0).getRight());
					
					}
				}
				
				if (myPosition.getLocationId()!=accessibleNode.getLocationId()) {
					this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					nextto.add(accessibleNode);
					
					
				}
			}


			if (!this.myMap.hasOpenNode()){
				//Explo finished on lance le behaviour de regroupement 
				
				finished=true;
				
				//on est bloqué
				if(this.silo == null) {
					this.myAgent.doDelete();
				}
				this.myAgent.addBehaviour(new RegroupementBehaviour(this.myAgent, this.myMap, this.list_agentNames, this.silo, this.tresorloc, this.tresorvalue));
				((AbstractDedaleAgent)this.myAgent).removeBehaviour(this);
				
			}else{
				
				if(this.path.size()<=0) {
					this.path = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId());
				}
				
				
				String nextNodeId=this.path.get(0);
				if (nextNodeId==null){

					nextNodeId = iter.next().getLeft().getLocationId();
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
					this.path = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId());
					
				}
				
			}

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
		if(this.myMap.hasOpenNode()) {
			
			
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("SHARE-TOPO");
			msg.setSender(this.myAgent.getAID());
			
			if(this.silo!=null) {
				msg.setContent(this.silo.getLocationId());
			}
			
			for (String agentName : this.list_agentNames) {
				
				
				System.out.println(this.talking);
				if(!this.talking.contains(agentName)) {
					System.out.println(agentName);
					msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
				}
			}
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
			
		}
		
	}

	@Override
	public boolean done() {
		return finished;
	}

}
