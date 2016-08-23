package dataFusionPlatform.server;
import static org.neo4j.helpers.collection.MapUtil.map;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;

import dataFusionPlatform.cypher.*;

/*
 * DFService is responsible for using the Cypher Executor to query the Neo4j server.
 * Various methods are set up that run different queries and return JSON parsable data.
 */
public class DFService 
{
	private final CypherExecutor cypher;
	// Constructor
	public DFService(String uri) 
	{
		cypher = createCypherExecutor(uri);
	}
	// creates and instance of the Cypher Executor with the given URL
	private CypherExecutor createCypherExecutor(String uri) 
	{
		try 
		{
			String auth = new URL(uri).getUserInfo();
	        if (auth != null) 
	        {
	        	String[] parts = auth.split(":");
	        	return new JdbcCypherExecutor(uri,parts[0],parts[1]);
	        }
	        return new JdbcCypherExecutor(uri);
		} catch (MalformedURLException e) 
		{
			throw new IllegalArgumentException("Invalid Neo4j-ServerURL " + uri);
	    }
	}
	

    // request handler for initially sending all dataset nodes to front end
	// limit defines the max length of the response. It is used as a parameter in requests
	public Map<String, Object> datasets(int limit) {
		// limit tells the neo4j server the max length of the response
		Iterator<Map<String,Object>> result = cypher.query(
    			"match (n:Dataset) return n as dataset, id(n) as id", 
    			map("1", limit));
		List<Map<String, Object>> datasets = new ArrayList<Map<String, Object>>();
		
		//result is essentially a collection of rows in a table of data returned by the query
		while (result.hasNext())
		{
			Map<String, Object> row = result.next();
			Map<String, Object> dataset = map("id", row.get("id"), "datasetNode", row.get("dataset"));
			datasets.add(dataset);
			
		}
		
		return map("datasets", datasets);
	}
	
	
	// request handler for obtaining the column and table nodes/edges of a dataset with the given ID
	public Map<String, Object> getDataset(int datasetID, int limit) {
		
		// limit tells the neo4j server the max length of the response
		Iterator<Map<String,Object>> result = cypher.query(
    			"start n=node(" + datasetID + ") match (n)<-[:BELONGS_TO]-(p)<-[:BELONGS_TO]-(c) "
    			+ "return n as dataset, labels(n)[0] as datasetType, ID(n) as datasetId, n.title as datasetName, p.title as parentName, labels(p)[0] as parentType, ID(p) as parentId, p as parent, c.title as childName, labels(c)[0] as childType, ID(c) as childId, c as child",
    			map("1",limit));
		
		
        
    	 List nodes = new ArrayList();
         List rels= new ArrayList();
         
         int i = 0;
         //Iterate through each row of the resulting cypher query
         while (result.hasNext()) 
         {
         	
         	//Row has a dataset, a table, and a collection of columns
         	Map<String, Object> row = result.next();
         	Map<String, Object> datasetNode = map("id", row.get("datasetId"), "name", row.get("datasetName"), "type", row.get("datasetType"), "properties", row.get("dataset"));
         	
         	//Dont need dtarget (Justin 4/5/15)
         	//int dtarget = i;
         	
         	//Check index for a dataset node, if it does not exist, add it to the 
         	//list of nodes
         	int dsource = nodes.indexOf(datasetNode);
         	if (dsource == -1)
         	{
         		nodes.add(datasetNode);
         		
         		//Increment and use dsource to create edges from a dataset 
         		//node to a Column or JoinColumn node (Justin 4/5/15)
         		dsource = i++;
         	}
         	
        	//Add the parent node if it is not already there
         	Map<String, Object> parentNode = map("id", row.get("parentId"), "name", row.get("parentName"), "type", row.get("parentType"), "properties", row.get("parent"));
         	int ptarget = nodes.indexOf(parentNode);
            if (ptarget == -1) 
            {
                nodes.add(parentNode);
                
                //Needed the increment of the parent index before adding 
                //the relation to a dataset node. Also use the counter dsource 
                //not dtarget (Justin 4/5/15)
                ptarget = i++;
                rels.add(map("source", ptarget, "target", dsource, "id","line"));

                
            }
            
         	//Add the child node if it is not already there
         	Map<String, Object> childNode = map("id", row.get("childId"), "name", row.get("childName"), "type", row.get("childType"), "properties", row.get("child"));
         	int source = nodes.indexOf(childNode);
            if (source == -1) 
            {
                nodes.add(childNode);
                source = i++;
            }
            // source and target are indices of their respective nodes in the nodes list
         	rels.add(map("source", source, "target", ptarget, "id","line"));
         	
         }
         return map("nodes", nodes, "links", rels);
         	
     }
	
	// request handler for obtaining nodes that have a certain property value 
	// the property parameter is used in the query to find nodes that have a 
	// specific value specified by propertyValue
	public Map<String, Object> matchProperty(String property, String propertyValue, int limit)
	{
		// limit tells the neo4j server the max length of the response
		Iterator<Map<String,Object>> result = cypher.query(
				"match (n:Column) where n." + property + " = \"" + propertyValue + "\" return n.title as name, labels(n)[0] as type, id(n) as id, n as node", 
				map("1", limit));
		
		List<Map<String, Object>> resultingNodes = new ArrayList<Map<String, Object>>();
		
		//result is essentially a collection of rows in a table of data returned by the query
		while (result.hasNext())
		{
			Map<String, Object> row = result.next();
			Map<String, Object> node = map("id", row.get("id"), "name", row.get("name"), "type", row.get("type"), "properties", row.get("node"));
         	
			resultingNodes.add(node);
			
		}
		
		return map("resultingNodes", resultingNodes);
	}
	

	
	
	// this method will run a cypher query that, given a column node id, will return the dataset, table and sibling columns of the given node
	public Map<String, Object> getTable(int nodeId, int limit)
	{	
		Iterator<Map<String,Object>> result = cypher.query(
				"start n=node(" + nodeId + ") " +
				"match (n)-->(p) " +
				"with id(p) as pid " +
				"match path=(n)-->(t)-->(d) " +
				"where id(t) = pid " +
				"unwind nodes(path) as r " +
				"return distinct r.title as name, labels(r)[0] as type, id(r) as id, r as node",
				map("1", limit));

		List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> links = new ArrayList<Map<String, Object>>();
		// list of indices in the node array that are column nodes 
		List<Integer> columnIndices = new ArrayList<>();
		int datasetIndex = 0, tableIndex = 0;
		int i = 0;
		
		while (result.hasNext())
		{
			Map<String, Object> row = result.next();
			Map<String, Object> node = map("id", row.get("id"), "name", row.get("name"), "type", row.get("type"), "properties", row.get("node"));
	     	String nodeType = (String) row.get("type");
	     	
			if (nodeType.equalsIgnoreCase("dataset")) { datasetIndex = i; }
			else if (nodeType.equalsIgnoreCase("table") || nodeType.equalsIgnoreCase("jointable")) { tableIndex = i; }
			else { columnIndices.add(i); }
			nodes.add(node);
			i++;
		}
		
		links.add(map("source", tableIndex, "target", datasetIndex, "id","line"));
		
		for (int cIndex : columnIndices)
		{
			links.add(map("source", cIndex, "target", tableIndex, "id","line"));
		}	
		
		return map("nodes", nodes, "links", links);	
	}
	
	
	// this method will run a cypher query that will return the parent table node's ID for any given column node
	public Map<String, Object> getTableIdForNode(int nId, int limit) {
		
		Iterator<Map<String,Object>> result = cypher.query(
				"start n=node(" + nId + ") " +
				"match (n)-->(p) " +
				"return id(p) as tableID",
				map("1", limit));
	
		Map<String, Object> row = null;
		
		while (result.hasNext())
		{
			row = result.next();
		}
		
		return map("idForTable", row.get("tableID"));
	}
		
	
}

