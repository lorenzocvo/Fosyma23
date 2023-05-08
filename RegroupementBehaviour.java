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
import eu.su.mas.dedaleEtu.mas.behaviours.TalkingCoopBehaviour;

import jade.core.Agent;
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
public class RegroupementBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	private List<String> list_agentNames;

	
	private List<String> path = new ArrayList<>();
	
	private List<String> talking = new ArrayList<>(); //ou talked to
	
	private List<Integer> cpt = new ArrayList<>();
	
	private List<Boolean> move = new ArrayList<>();
	
	private Location silo;
	
	private List<String> tresorloc = new ArrayList<>();
	
	private List<Integer> tresorvalue = new ArrayList<>();
	
	private int tick = 0;
	
	private boolean arrived = false;
	
	private List<String> agentsarrived = new ArrayList<>();

/**
 * 
 * @param myagent
 * @param myMap known map of the world the agent is living in
 * @param agentNames name of the agents to share the map with
 */
	public RegroupementBehaviour(final Agent myagent, MapRepresentation myMap,List<String> agentNames, Location siloloc, List<String>tresloc, List<Integer>tresval) {
		super(myagent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
		this.move.add(true);
		this.silo = siloloc;
		this.tresorloc = tresloc;
		this.tresorvalue = tresval;
		
		
		//on ne veut pas relayer son propre message
		this.agentsarrived.add(this.myAgent.getLocalName());
		
		
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		this.path = this.myMap.getShortestPath(myPosition.toString(), this.silo.toString());
		
		
	}

	@Override
	public void action() {
		
		//attention à ne pas couper en pleine communication
		this.tick++;
		
		
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		
		
		try {
			this.myAgent.doWait(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//optimiser : d'abord on regarde si on a reçu, puis on bouge, puis on envoie
		
		MessageTemplate msgTemplate=MessageTemplate.MatchAll();
		ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
		
		
		
		
		if(msgReceived==null) {
			
		}else if (!this.talking.contains(msgReceived.getSender().getLocalName()) && (msgReceived.getProtocol()=="SHARE-TOPO" || msgReceived.getProtocol()=="ASK-MAP" ) ) {
			System.out.println(msgReceived);
		
			this.talking.add(msgReceived.getSender().getLocalName());
			this.cpt.add(0);
			
			
			ACLMessage msg = msgReceived.createReply();
			msg.setProtocol("FULL-MAP");
			msg.setSender(this.myAgent.getAID());
				
				
					
			SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
			try {					
				msg.setContentObject(sg);
			} catch (IOException e) {
					e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
			
		}else if(msgReceived.getProtocol()=="ASK-ARRIVED" && !this.move.get(0)) {
			
			ACLMessage msg = msgReceived.createReply();
			msg.setProtocol("ARRIVED");
			msg.setSender(this.myAgent.getAID());
				
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
		}else if (msgReceived.getProtocol()=="ARRIVED"  ) {
			
			this.move.set(0, false);
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("INFORMATIONS");
			msg.setSender(this.myAgent.getAID());
			
			String message = "";
			message = message + this.myAgent.getLocalName() + "/" + ((AbstractDedaleAgent)this.myAgent).getBackPackFreeSpace().get(0).getRight().toString() + "/";
			
			for(int i =0;i<this.tresorloc.size();i++) {
			
				message = message + this.tresorloc.get(i) + ",";
				
			}
				
			message = message.substring(0, message.length()-1);
			message = message + "/";
			
			for(int i =0;i<this.tresorvalue.size();i++) {
				
				message = message + this.tresorvalue.get(i) + ",";
				
			}
			
			message = message.substring(0, message.length()-1);
			
			msg.setContent(message);
			
			for (String agentName : this.list_agentNames) {
				
				
				System.out.println(this.talking);
				if(!this.talking.contains(agentName)) {
					System.out.println(agentName);
					msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
				}
			}
			
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
		}else if (msgReceived.getProtocol()=="INFORMATIONS" && !this.move.get(0) ) {
			
			
			List<String> tmp = new ArrayList<String>(Arrays.asList(msgReceived.getContent().split("/")));

			System.out.println(this.agentsarrived.toString()+"agents arrived");
			System.out.println("tmp :"+tmp.get(0));
			if(!this.agentsarrived.contains(tmp.get(0))) {
			
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setProtocol("INFORMATIONS");
				msg.setSender(this.myAgent.getAID());
					
				
				msg.setContent(msgReceived.getContent());
				
							
				this.agentsarrived.add(tmp.get(0));
				
				for (String agentName : this.list_agentNames) {
						
					//peut etre mettre un while pour ne pas flooder le réseau
					
					msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
					
				}
					
				((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			}
				
			
		}else if(msgReceived.getProtocol()=="FIRST-ARRIVED") {
			
			this.move.set(0, false);
			ACLMessage msg = msgReceived.createReply();
			msg.setProtocol("FULL-MAP");
			msg.setSender(this.myAgent.getAID());
				
				
					
			SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
			try {					
				msg.setContentObject(sg);
			} catch (IOException e) {
					e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
			
			
		}else if(msgReceived.getProtocol()=="LAUNCH-RECOLTE") {
			
			
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("LAUNCH-RECOLTE");
			msg.setSender(this.myAgent.getAID());
			msg.setContent(msgReceived.getContent());
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
			System.out.println(msgReceived.getContent());
			
			finished=true;
			
			this.myAgent.addBehaviour(new RecolteBehaviour(this.myAgent, this.myMap, this.list_agentNames,msgReceived.getContent(),this.silo));
			((AbstractDedaleAgent)this.myAgent).removeBehaviour(this);
			
		}
		
		
		if(this.move.get(0)) {
		
			String nextNodeId;
			
			if(myPosition!=this.silo) {
				nextNodeId=this.path.get(0);
				if(((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
					this.path.remove(0);
				}else {
					

					System.out.println("Je suis arrivé?");
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setProtocol("ASK-ARRIVED");
					msg.setSender(this.myAgent.getAID());
					msg.setContent("test");
					
					for (String agentName : this.list_agentNames) {
						
						if(!this.talking.contains(agentName)) {
							
							msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
						}
					}
					
					((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
				}
				
			}else {
			
				this.move.set(0,false);
				this.arrived = true;
				
				
			}
			
		}
		
		
	}
	

	@Override
	public boolean done() {
		return finished;
	}

}
