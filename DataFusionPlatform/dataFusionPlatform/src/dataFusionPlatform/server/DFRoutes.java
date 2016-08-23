 package dataFusionPlatform.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dataFusionPlatform.server.DFService;
import dataFusionPlatform.utility.*;
import spark.servlet.SparkApplication;
import spark.Request;
import spark.Response;
import spark.Route;
import static spark.Spark.get;



/*
 * DFRoutes defines our application's endpoints - where the web browser interacts with our backend.
 * The init() method is the first method to run in our application's back end.  It sets up the URLs
 * that the browser can request via HTTP and sends the resulting data as a response.  Inside the init
 * method, we can define handlers for the various types of HTTP requests such as GET, POST, etc.
 * The actions to be taken within the handlers are delegated to the DFService class.  DFService has 
 * various methods that correlate to these routes.
 */
public class DFRoutes implements SparkApplication{

	private Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private DFService service = new DFService(Util.getNeo4jUrl());

	@Override
	public void init() 
	{

		
		// handles a get request to the route /datasets
		// it is initially requested when the webpage loads in order to give the user a list of datasets to start from
		get("/datasets", new Route() {
            public Object handle(Request request, Response response) {
            	// limit defines a limit on the length of the response if it is not defined in the request
            	int limit = request.queryParams("limit") != null ? Integer.valueOf(request.queryParams("limit")) : 100;
                // The gson.toJson simply converts the given data to JSON format for sending it in an HTTP response.
            	// Here we are calling the datasets method in the service object which will query neo4j for all the 
            	// datasets in the database.
            	return gson.toJson(service.datasets(limit));
               
            }
        }); 
		
		// handles a request to the route /getDataset/536 for example
		// :datasetID is a parameter for any value that follows the second slash
		// It is expected to be an integer value that is the ID number of a user-chosen dataset
		get("/getDataset/:datasetID", new Route() {
            public Object handle(Request request, Response response) {
            	// limit defines a limit on the length of the response if it is not defined in the request
            	int limit = request.queryParams("limit") != null ? Integer.valueOf(request.queryParams("limit")) : 100;
            	// we must convert the datasetID to a integer since it comes in as a string
                int dID = Integer.parseInt(request.params(":datasetID"));
                // The gson.toJson simply converts the given data to JSON format for sending it in an HTTP response.
            	// Here we are calling the getDataset method in the service object which will query neo4j for the 
            	// table and column nodes and edges of a dataset with the given ID.
            	return gson.toJson(service.getDataset(dID, limit));
               
            }
        });
		
		// handles a request to the route: /getTableIdForNode/547 for example
		// it will return the table node ID for any given column node ID
		get("/getTableIdForNode/:nodeID", new Route() {
            public Object handle(Request request, Response response) {
            	int limit = request.queryParams("limit") != null ? Integer.valueOf(request.queryParams("limit")) : 100;
                int nID = Integer.parseInt(request.params(":nodeID"));
            	return gson.toJson(service.getTableIdForNode(nID, limit));
               
            }
        });
		
		// handles a request to the route /matchProperty/represents/mesh:Disease for example
		// :property is the node/column property that the user wants to use to find other nodes 
		// having a certain :propertyValue
		get("/matchProperty/:property/:propertyValue", new Route() {
            public Object handle(Request request, Response response) {
            	// limit defines a limit on the length of the response if it is not defined in the request
            	int limit = request.queryParams("limit") != null ? Integer.valueOf(request.queryParams("limit")) : 100;
                String prop = request.params(":property");
                String propVal = request.params(":propertyValue");
                // The gson.toJson simply converts the given data to JSON format for sending it in an HTTP response.
            	// Here we are calling the matchProperty method in the service object which will query neo4j for all the 
            	// nodes that have a :property with a certain :propertyValue
                return gson.toJson(service.matchProperty(prop, propVal, limit));
               
            }
        });
		
		//handles a request to the route /getTable/541 for example
		// :nodeID is a parameter for any node that is a COLUMN
		// this route will run a cypher query that will return the given column node's:
		//		1. parent table node
		//		2. grandparent dataset node
		//		3. sibling column nodes
		get("/getTable/:nodeID", new Route() {
            public Object handle(Request request, Response response) {
            	// limit defines a limit on the length of the response if it is not defined in the request
            	int limit = request.queryParams("limit") != null ? Integer.valueOf(request.queryParams("limit")) : 100;
            	
            	int nID = Integer.parseInt(request.params(":nodeID"));
                // The gson.toJson simply converts the given data to JSON format for sending it in an HTTP response.
            	// Here we are calling the getTable method which in the service object which will query neo4j for  
            	// a given column node ID's 1. parent table node 2. grandparent dataset node 3. sibling column nodes
            	return gson.toJson(service.getTable(nID, limit));
               
            }
        }); 
		
	}
}
