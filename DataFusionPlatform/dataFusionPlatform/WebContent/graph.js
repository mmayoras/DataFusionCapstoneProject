//var width = 1200, height = 800, radius = 20, rTable = 30, rDataset = 40;
var radius = 20, rTable = 30, rDataset = 40;
var margin = {top: -5, right: -5, bottom: -5, left: -5};
var width = 1200 - margin.left - margin.right, height = 800 - margin.top - margin.bottom;

var force = d3.layout.force()
    .charge(-1000)
    .linkDistance(100)
    .size([width, height]);
//[width, height] [width + margin.left + margin.right, height + margin.top + margin.bottom]

//Test zoom functionality
// create the zoom listener
var zoom = d3.behavior.zoom()
    .scaleExtent([1, 15])
    .on("zoom", zoomed);

var drag = d3.behavior.drag()
    .origin(function(d) { return d; })
    .on("dragstart", dragstarted)
    .on("drag", dragged)
    .on("dragend", dragended);

var svg = d3.select("#graph")
    .append("svg")
    .attr("width", "100%")
    .attr("height", "100%")
    .attr("pointer-events", "all")
    .attr("transform", "translate(" + margin.left + "," + margin.right + ")")
    .call(zoom);

var container = svg.append("g");

d3.json("/Justin/graph", function(error, graph) {

    if (error) return;
    
    force.nodes(graph.nodes).links(graph.links).start();

    var link = container.append("g")
    	.selectAll(".link")
        .data(graph.links).enter()
        .append("line").attr("class", "link");

    //Modified D3 nodes on 3/27/15 By Justin 
    //Nodes are now a container that contains a circle graphic and its title
    //Each node creates a "g" element container and appends:
   	//	1: SVG Circle
   	//	2: Text displaying title of node
    var node = container.append("g")
    	.selectAll(".node")
    	.data(graph.nodes)
    	.enter().append("g")
    	.attr("class", function (d) { return "node "+ d.type.toString(); })
    	.style("fill", function(d) {return d.colr; })
    	.call(drag);
    
    //Add a SVG circle element to the node container	
    node.append("circle")
    	//Dynamically adjust the size of circles depending on its type
    	.attr("r", function (d) {
    		switch (d.type.toString()) {
    			case "Dataset":		return 40;
    			case "Table":		return 30;
    			case "JoinTable":	return 30;
    			default:			return 20;
    		}
    	})
    	
    //Add a Title element to display nodes title container
    node.append("text")
    	//Adjust the placement of text on the X-AXIS for displaying the title
    	.attr("dx", function (d) {
    		switch (d.type.toString()) {
    			case "Dataset":		return 40;
    			case "Table":		return 30;
    			case "JoinTable":	return 30;
    			default:			return 20;
    		}
    	})
    	.attr("dy", ".35em")
        .text(function (d) { return d.name; })
        
         
    var state = false;
    var last = null;
    var current = null;
    node.on("click", function(n) {
        //Return color of nodes back to normal
        svg.selectAll(".node").style("fill", function(d) { return d.colr; });
        
        var getOptionsDiv = document.getElementById("displayOptions");
        while (getOptionsDiv.hasChildNodes()) { 
            getOptionsDiv.removeChild(getOptionsDiv.lastChild);
        }
              
        //Get Represents property from currently selected node
        currRepresents = n.properties.represents;
        
        //Add data to meta info div
        var metainf = "";
        metainf = metainf.concat("Title: ", n.name, "<br/>Label: ", n.type, "<br/>Represents: ", n.properties.represents, 
        "<br/>Column Type: ", n.properties.columntype, "<br/>Semantic Relation: ", n.properties.semanticrelation);
        console.log(metainf);
        d3.select("#metainfo")
            .html(metainf);
        
        last = current;
        current = d3.select(this);
        current.style('fill', 'red');
        last.style('fill', function(d) { return d.colr; });
    
        getTitle = n.properties.title;
        getRepresents = n.properties.represents;
        getColumnType = n.properties.columntype;
        getSemanticRelation = n.properties.semanticrelation;

        function createButton(label, functionCall) {
            var btn = document.createElement("BUTTON"); //Create the button element
            var title = document.createTextNode(label); //Create the button label, and add it to the button
            btn.appendChild(title);
            btn.onclick = functionCall; //Call function when button is clicked
            document.getElementById("displayOptions").appendChild(btn); //Add button to the 'displayOptions' div inside the console
        }
        
        //Dynamically create button for finding related Titles, Represents, Column Types, Relations
        if (getTitle !== undefined)            { createButton("Find Related Titles", findTitle); }
        if (getRepresents !== undefined)       { createButton("Find Related Represents", findRep); }
        if (getColumnType !== undefined)       { createButton("find Related Column Types", findColType); }
        if (getSemanticRelation !== undefined) { createButton("Find Related Semantic Relations", findSemRel); }
    });

    // force feed algo ticks
    force.on("tick", function() {
        node.attr("cx", function(d) { return d.x = Math.max(radius, Math.min(width - radius, d.x)); })
        .attr("cy", function(d) { return d.y = Math.max(radius, Math.min(height - radius, d.y)); });
        
        link.attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });

        node.attr("cx", function(d) { return d.x; })
            .attr("cy", function(d) { return d.y; });
        
        node.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });
    });

});

// titles
// column types
// represents
// semantic relation

function search() {
    var arr = [];
    var checks = document.getElementsByClassName("check");
    for (i = 0; i < checks.length; i++) {
        if (checks[i].checked) {
            arr.push(checks[i].name);
        }
    }

    // var arr = ["title", "represents", "columntype", "semanticrelation"];
    for (i = 0; i < arr.length; i++) {
        att = arr[i];
        var userinput = document.getElementById("searchbox").value;
        "string".toLowerCase();
        d3.selectAll(".node")
            .filter(function(d) {
                if (d.properties[att] != null) {
                    return d.properties[att].toLowerCase().indexOf(userinput.toLowerCase()) > -1;
                }
                else {
                    return false;
                }
            })
            .style('fill', "teal");
    }
}



// function for handling zoom event
function zoomed() {
    container.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")");
}

function dragstarted(d) {
    d3.event.sourceEvent.stopPropagation();
    d3.select(this).classed("dragging", true);
    force.start();
}

function dragged(d) {
    d3.select(this).attr("cx", d.x = d3.event.x).attr("cy", d.y = d3.event.y);
}

function dragended(d) {
    d3.select(this).classed("dragging", false);
}

function find(att, match, color) {
    svg.selectAll(".node").style("fill", function(d) { return d.colr; });
    //Filter through all nodes to find matches, color them appropriately
    svg.selectAll(".node")
    .transition()
    .filter(function(d) { return d.properties[att] == match; })
    .style('fill', color);
}

function findTitle()   { match("title", getTitle, "yellow"); }
function findRep()     { match("represents", getRepresents, "blue"); }
function findColType() { match("columntype", getColumnType, "green"); }
function findSemRel()  { match("semanticrelation", getSemanticRelation, "orange"); }
