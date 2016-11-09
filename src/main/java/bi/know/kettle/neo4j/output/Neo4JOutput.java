package bi.know.kettle.neo4j.output;

import java.net.URI;
import java.util.Arrays;

import javax.ws.rs.core.MediaType;

import org.neo4j.graphdb.Transaction;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIFacade;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;


public class Neo4JOutput  extends BaseStep implements StepInterface {
	private static Class<?> PKG = Neo4JOutput.class; // for i18n purposes, needed by Translator2!!

	private Neo4JOutputMeta meta; 
	private Neo4JOutputData data;
	private RestAPI graphDb; 
	private Transaction tx;
	private int nbRows;
	private String[] fieldNames;
	private static String SERVER_URI; 
	private Object[] r; 
	private Object[] outputRow;
	
    public Neo4JOutput(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
		super(s,stepDataInterface,c,t,dis);
	}
    
    
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException{
    	
    	meta = (Neo4JOutputMeta)smi;
    	data =  (Neo4JOutputData)sdi;
    	
    	r = getRow();
    	
        if (first){
            first = false;
            nbRows= 0;
            
	         data.outputRowMeta = (RowMetaInterface)getInputRowMeta().clone();
          	data.outputRowMeta.addValueMeta(new ValueMetaString("nodeURI"));
   	        fieldNames =  data.outputRowMeta.getFieldNames(); 
        }
        
        if(r == null){
        	setOutputDone();
        	return false; 
        }else{
        	try{ 
   	     	 outputRow = RowDataUtil.resizeArray( r, data.outputRowMeta.size());

   	     	 // create node
   	     	String nodeId = createNode(meta.getKey(), (String)r[Arrays.asList(fieldNames).indexOf(meta.getKey())]); 
        	putRow(data.outputRowMeta, outputRow);
        	nbRows++;
        	setLinesWritten(nbRows);
        	setLinesOutput(nbRows);
        	
        	// add label
        	String nodeLabels[] = meta.getNodeLabels();
        	for(int i=0; i < nodeLabels.length; i++){
        		addNodeToLabel(nodeId, (String)r[Arrays.asList(fieldNames).indexOf(nodeLabels[i])]); 
        	}
        	        	
        	// add properties
        	String nodeProps[] = meta.getNodeProps();
        	for(int i=0; i < nodeProps.length; i++){
        		addNodeProperty(nodeId, nodeProps[i], (String)r[Arrays.asList(fieldNames).indexOf(nodeProps[i])]);
        	}
        	
        	}catch(Exception e){
        		logError(BaseMessages.getString(PKG, "Neo4JOutput.addNodeError") + e.getMessage());
        	}
        	
        	try{
      	     	 // Create relationship 
        	 	String[][] relationships = meta.getRelationships(); 
        	 	for(int i=0; i < relationships.length; i++){
//        	 		System.out.println("####################################");
//        	 		System.out.println(Arrays.toString(relationships[i]));
        	 		String fromField = relationships[i][0]; 
        	 		String fromFieldVal = (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][0])];
        	 		String fromProp = relationships[i][1];
        	 		String fromPropVal = (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][1])];
        	 		String relLabel = relationships[i][2];
        	 		String relLabelVal = (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][2])];
        	 		String toField = relationships[i][3];
        	 		String toFieldVal = (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][3])];
        	 		String toProp = relationships[i][4];
        	 		String toPropVal = (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][4])];
        	 		
//        	 		System.out.println("fromField: " + fromField);
//        	 		System.out.println("fromFieldVal: " + fromFieldVal);
//        	 		System.out.println("fromProp: " + fromProp);
//        	 		System.out.println("fromPropVal: " + fromPropVal);
//        	 		System.out.println("relLabel: " + relLabel);
//        	 		System.out.println("relLabelVal: " + relLabelVal);
//        	 		System.out.println("toField: " + toField);
//        	 		System.out.println("toFieldVal: " + toFieldVal);
//        	 		System.out.println("toProp: " + toProp);
//        	 		System.out.println("toPropVal: " + toPropVal);
//        	 		
        	 		
//        	 		System.out.println("From Node: " + createNode(relationships[i][1], (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][1])]));
//        	 		System.out.println("From Node: " + createNode(fromPropVal, fromFieldVal));
//        	 		System.out.println("From Node: " + createNode(toPropVal, toFieldVal));
        	 		String fromNodeId = createNode(fromPropVal, fromFieldVal);
        	 		String toNodeId = createNode(toPropVal, toFieldVal);
        	 		String relationshipId = createRelationship(fromNodeId, toNodeId, relLabelVal);
    	       		addRelationshipProperty(relationshipId, meta.getRelProps());
//        	 		System.out.println("####################################");
//        	 		String fromNodeId = createNode(relationships[i][0], (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][0])]);
//        	 		System.out.println("############################################");
//        	 		System.out.println(relationships[i][2] + " --" +  (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][2])]);
//        	 		System.out.println("############################################");
//        	 		String toNodeId = createNode(relationships[i][2], (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][2])]);
//        	 		String relationshipId = createRelationship(fromNodeId, toNodeId, (String)r[Arrays.asList(fieldNames).indexOf(relationships[i][1])]);
//    	       		addRelationshipProperty(relationshipId, meta.getRelProps());
        	 	}
            	
        	}catch(Exception e){
        		logError(BaseMessages.getString(PKG, "Neo4JOutput.addRelationshipError") + e.getMessage());
        	}
        	return true; 
        }
    }
    
    
	public boolean init(StepMetaInterface smi, StepDataInterface sdi){
	    meta = (Neo4JOutputMeta)smi;
	    data = (Neo4JOutputData)sdi;
	    
	    SERVER_URI = meta.getProtocol() + "://" + meta.getHost() + ":" + meta.getPort() + "/db/data/";
	    graphDb = new RestAPIFacade(SERVER_URI, meta.getUsername(), meta.getPassword());	    
	    tx = graphDb.beginTx();  
	    
	    return super.init(smi, sdi);
	}
	
	public void dispose(StepMetaInterface smi, StepDataInterface sdi){
	    tx.success();  
	    tx.close(); 
	    super.dispose(smi, sdi);
	}

    
    public String createNode(String nodeKey, String nodeValue){
    	String nodeJSON = "{\"key\": \"" + nodeKey + "\", \"value\" : \"" + nodeValue  +   "\"}";    
    	System.out.println("nodeJSON: " + nodeJSON);
    	Client c = Client.create();
    	c.addFilter(new HTTPBasicAuthFilter(meta.getUsername(), meta.getPassword()));
	    WebResource nodeResource = c.resource(SERVER_URI + "index/node/pdi?uniqueness=get_or_create");    	
    	ClientResponse nodeResponse = nodeResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).entity(nodeJSON).post(ClientResponse.class);
    	final URI nodeLocation = nodeResponse.getLocation();
    	outputRow[data.outputRowMeta.size()-1] = nodeLocation.toString();
    	if(nodeResponse.getStatus() == 201 || nodeResponse.getStatus() == 200){
    		logDetailed(BaseMessages.getString(PKG, "Neo4JOutput.addNodeLabel") + nodeLocation);
        	return getIdFromURI(nodeLocation);
    	}else{
    		// throw exception.
    		logError(BaseMessages.getString("Neo4JOutput.addNodeError"));
    		return ""; 
    	}
    }
    
    
    private int addNodeToLabel(String nodeId, String labelStr){
    	try{
        	URI labelURI = new URI(SERVER_URI + "node/" + nodeId + "/labels");
        	Client labelClient = Client.create();
        	labelClient.addFilter(new HTTPBasicAuthFilter(meta.getUsername(), meta.getPassword()));
    	    WebResource labelResource = labelClient.resource(labelURI);
    		String labelJSON = "\"" + labelStr + "\"";
        	ClientResponse labelResponse = labelResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).entity(labelJSON).post(ClientResponse.class);
        	final URI labelLocation = labelResponse.getLocation();
        	logDetailed(BaseMessages.getString(PKG, "Neo4JOutput.addNodeLabel") + labelLocation);
        	return 0; 
    	}catch(Exception e){
    		logError(BaseMessages.getString(PKG, "Neo4JOutput.addNodeLabelError") + nodeId);
    		return -1; 
    	}
    }
    
    
    private int addNodeProperty(String nodeId, String propKey, String propValue){
    	try{
        	URI propertyURI = new URI(SERVER_URI + "node/" + nodeId + "/properties/" + propKey);
        	Client propertyClient = Client.create();
        	propertyClient.addFilter(new HTTPBasicAuthFilter(meta.getUsername(), meta.getPassword()));
    	    WebResource propertyResource = propertyClient.resource(propertyURI);
    		String propertyJSON = "\"" + propValue + "\"";
        	ClientResponse propertyResponse = propertyResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).entity(propertyJSON).put(ClientResponse.class);
        	final URI propertyLocation = propertyResponse.getLocation();
        	logDetailed(BaseMessages.getString(PKG, "Neo4JOutput.addNodeProperty") + propertyLocation);
        	return 0; 
    	}catch(Exception e ){
        	logError(BaseMessages.getString(PKG, "Neo4JOutput.addNodePropertyError") + nodeId);
        	return -1; 
    	}
    }
    
    private String createRelationship(String fromNodeId, String toNodeId, String relationshipType){
    	logDetailed("creating relationship from " + fromNodeId + " to " + toNodeId + " , type: " + relationshipType); 
    	try{
     		URI fromNodeURI = new URI(SERVER_URI + "node/" + fromNodeId + "/relationships");
           	URI toNodeURI = new URI(SERVER_URI + "node/" + toNodeId);
           	String  relationshipJSON = "{ \"to\" : \"" + toNodeURI + "\", \"type\" : \"" +  relationshipType + "\"}" ;
           	Client fromNodeClient = Client.create(); 
        	fromNodeClient.addFilter(new HTTPBasicAuthFilter(meta.getUsername(), meta.getPassword()));
           	WebResource fromNodeResource = fromNodeClient.resource(fromNodeURI);
           	ClientResponse relationshipResponse = fromNodeResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).entity(relationshipJSON).post(ClientResponse.class);
           	final URI relationshipURI = relationshipResponse.getLocation();
        	logDetailed(BaseMessages.getString(PKG, "Neo4JOutput.addRelationship") + fromNodeId + " - " + toNodeId + " - " + relationshipType + " - " + relationshipURI + " - " + relationshipResponse.getStatus() + " - " + relationshipJSON);
        	return getIdFromURI(relationshipURI);
    	}catch(Exception e){
        	logError(BaseMessages.getString(PKG, "Neo4JOutput.addRelationshipError") + fromNodeId + ", "  + toNodeId  + ", " + relationshipType);
        	return ""; 
    	}
    }
    
    
//    private int addRelationshipProperty(String relationshipId, String propKey, String propValue){
      private int addRelationshipProperty(String relationshipId, String[] relProps){
    	try{
    		String relPropsJSON = "{";
    		for(int i=0; i < relProps.length; i++){
    			relPropsJSON += "\"" + relProps[i] + "\" : \"" +  (String)r[Arrays.asList(fieldNames).indexOf(relProps[i])] + "\" ";
    			if(i <  (relProps.length-1)){
    				relPropsJSON += ", ";
    			}
    		}
    		relPropsJSON += "}" ;
	       	Client relPropsClient = Client.create(); 
        	relPropsClient.addFilter(new HTTPBasicAuthFilter(meta.getUsername(), meta.getPassword()));
        	String relationshipURI = SERVER_URI +  "relationship/" + relationshipId + "/properties"; 
	       	WebResource relPropsResource = relPropsClient.resource(relationshipURI);
	       	ClientResponse relPropsResponse = relPropsResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).entity(relPropsJSON).put(ClientResponse.class);
	       	final URI relPropsURI = relPropsResponse.getLocation();
        	logDetailed(BaseMessages.getString(PKG, "Neo4JOutput.addRelationshipProperty") + relPropsURI);
	       	return 0; 
    	}catch(Exception e){
        	logDetailed(BaseMessages.getString(PKG, "Neo4JOutput.addRelationshipPropertyError") + relationshipId);
        	return -1; 
    	}
    }
    
    private String getIdFromURI(URI uri){
    	return uri.toString().substring(uri.toString().lastIndexOf("/")+1);
    }

    
}
