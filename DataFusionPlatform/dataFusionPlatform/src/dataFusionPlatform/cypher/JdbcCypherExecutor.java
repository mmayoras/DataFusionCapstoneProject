package dataFusionPlatform.cypher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// credit for this class goes to Michael Hunger

// class used for querying the neo4j database using JDBC drivers
public class JdbcCypherExecutor implements CypherExecutor
{
	 private final Connection conn;

	    public JdbcCypherExecutor(String url) 
	    {
	        this(url,null,null);
	    }
	    public JdbcCypherExecutor(String url,String username, String password) 
	    {
	        try 
	        {
	        	Class.forName("org.neo4j.jdbc.Driver");
	            conn = DriverManager.getConnection(url.replace("http://","jdbc:neo4j://"),username,password);
	        } catch (SQLException e) 
	        {
	            throw new RuntimeException(e);
	        } catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
	        	throw new RuntimeException(e);
			}
	    }

	    // the query parameter is a CYPHER query string and params holds any parameters necessary for the query
	    @Override
	    public Iterator<Map<String, Object>> query(String query, Map<String, Object> params) 
	    {
	        try 
	        {
	            final PreparedStatement statement = conn.prepareStatement(query);
	            setParameters(statement, params);
	            final ResultSet result = statement.executeQuery();
	            return new Iterator<Map<String, Object>>() 
	            {

	                boolean hasNext = result.next();
	                public List<String> columns;

	                @Override
	                public boolean hasNext() 
	                {
	                    return hasNext;
	                }

	                private List<String> getColumns() throws SQLException 
	                {
	                    if (columns != null) return columns;
	                    ResultSetMetaData metaData = result.getMetaData();
	                    int count = metaData.getColumnCount();
	                    List<String> cols = new ArrayList<>(count);
	                    for (int i = 1; i <= count; i++) cols.add(metaData.getColumnName(i));
	                    return columns = cols;
	                }

	                @Override
	                public Map<String, Object> next() 
	                {
	                    try 
	                    {
	                        if (hasNext) 
	                        {
	                            Map<String, Object> map = new LinkedHashMap<>();
	                            for (String col : getColumns()) map.put(col, result.getObject(col));
	                            hasNext = result.next();
	                            if (!hasNext) 
	                            {
	                                result.close();
	                                statement.close();
	                            }
	                            return map;
	                        } else throw new NoSuchElementException();
	                    } catch (SQLException e) 
	                    {
	                        throw new RuntimeException(e);
	                    }
	                }

	                @Override
	                public void remove() 
	                {
	                }
	            };
	        } catch (SQLException e) 
	        {
	            throw new RuntimeException(e);
	        }
	    }

	    private void setParameters(PreparedStatement statement, Map<String, Object> params) throws SQLException 
	    {
	        for (Map.Entry<String, Object> entry : params.entrySet()) 
	        {
	            int index = Integer.parseInt(entry.getKey());
	            statement.setObject(index, entry.getValue());
	        }
	    }
	}

