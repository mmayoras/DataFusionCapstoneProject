package dataFusionPlatform.utility;

// Utility class for setting up server port and neo4j connection
public class Util 
{
	// this line defines the URL at which the Neo4j instance is running
	public static final String OLD_DEFAULT_URL = "http://server2.ies.cse.ohio-state.edu:8081";
	public static final String DEFAULT_URL = "http://localhost:7474";
    public static final String WEBAPP_LOCATION = "WebContent/";

    public static int getWebPort() 
    {
        String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) 
        {
            return 8080;
        }
        return Integer.parseInt(webPort);
    }

    public static String getNeo4jUrl() 
    {
        String urlVar = System.getenv("NEO4J_REST_URL");
        if (urlVar==null) urlVar = "NEO4J_URL";
        String url =  System.getenv(urlVar);
        if(url == null || url.isEmpty()) 
        {
            return DEFAULT_URL;
        }
        return url;
    }
}
