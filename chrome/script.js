// setup the websocket
var socket = new WebSocket('wss://127.0.0.1:8080/');
	  
// Handle any errors that occur.
socket.onerror = function(error) {
	console.log('WebSocket Error: ' + error);
};

// Make sure we're connected to the WebSocket before trying to send anything to the server
socket.onopen = function(event) {
	// send the code example to the backend for parsing and analysis
	console.log('Connected to the server.');
}

// Show a disconnected message when the WebSocket is closed.
socket.onclose = function(event) {
	console.log('Disconnected from the server.');
};

var cloneStacks = null;
var currentStack = null;

// Handle messages sent by the backend.
socket.onmessage = function(event) {
	console.log(event.data);

	var message = event.data;
	
	if (message.startsWith("ClonedSnippets: ")) {
		message = message.substring(16);
		highlightClonedSnippet(message);
	} else if (message.startsWith("CloneStacks: ")) {
		message = message.substring(13);
		// store the clone stacks for further display as a user clicks
		cloneStacks = JSON.parse(message);
	} else if(message.startsWith("Update: ")) {
		message = message.substring(8);
		
		// remove the existing side bar
		var el = document.getElementById('mySidebar');
		el.parentNode.removeChild(el);
		var clonestack = JSON.parse(message);
		displaySideBar(clonestack);
	}
}

function highlightClonedSnippet(clonedSnippets) {
	var url = window.location.href;
	var postId = url.substring(url.indexOf("#") + 1);
	var post = document.getElementById("answer-" + postId);
	var codeElements = post.getElementsByTagName("code");
	
	var jsonSnippets = JSON.parse(clonedSnippets);
	for(var id in jsonSnippets) {
		var snippet = jsonSnippets[id];
		var elem = codeElements[id];
		elem.innerHTML = PR.prettyPrintOne(snippet);
	}

	// add a click listener to each highlighted SO method
	$('.clone').click(function() {
		console.log("Click on Clone#" + this.id);

		if(cloneStacks == null) {
			alert("Have not received the GitHub clones from the server. Please check later.");
			return;
		}
		
		// notify the server
		socket.send("ClickedClone: " + this.id);

		// highlight the current cloned method
		this.style = "background-color: #ffff99; border-style: dashed; border-width: 3px; border-color: red;";
		
		if(currentStack != null) {
			// remove the previous highlighted cloned method
			var clones = document.getElementsByClassName("clone");
			for(var i = 0; i < clones.length; i++) {
				var clone = clones[i];
				if(clone.id == currentStack.id) {
					clone.style = "background-color: #ffff99;";
					break;
				}
			}

			// remove the existing side bar
			var el = document.getElementById('mySidebar');
			el.parentNode.removeChild(el);
		}

		// grab the clone stack based on its id
		for(var index in cloneStacks) {		
			var clonestack = cloneStacks[index];
			if(clonestack.id == this.id) {
				// this is the cloned SO method that the user clicks on, display it
				displaySideBar(clonestack);
				sidebarOpen = true;
				break;
			}
		}
	});
}

// generate the clone stack on the fly
function displaySideBar(clonestack) {
	var sidebar = document.createElement('div');  
	sidebar.id = "mySidebar";

	var content = "\
			<h1>CodeAid: Related Code Recommendation</h1>\
			<div class=\"stack\">\ ";

	// set the current stack 
	currentStack = clonestack;
	// this is the cloned SO method that the user clicks on
	// add the code template
	content += "<h2>" + clonestack.clones.length + " Related Code Fragments in GitHub</h2>";
	// add the similar GitHub clones
	for(var k in clonestack.clones) {
		// add a GitHub clone with highlighted diff regions
		var clone = clonestack.clones[k];
		content += "<a href=\"" + clone.url + "\" target=\"_blank\">GitHub link</a>&nbsp;&nbsp;<span>watch: " + clone.watch + "</span>&nbsp;&nbsp;<span>star: " + clone.star + "</span>&nbsp;&nbsp;<span>fork: " + clone.fork + "</span>\
			<div id=\"" + k + "\" class=\"gh-example\"><pre class=\"fix\"><code>" + PR.prettyPrintOne(clone.ghCode) + "</code></pre></div>";
	}

	content += "</div>\
		";
		
	sidebar.innerHTML = content;
	sidebar.style.cssText = "\
		position:fixed;\
		top:0px;\
		right:0px;\
		width:35%;\
		height:100%;\
		background:white;\
		box-sizing: border-box;\
		border: 2px solid black;\
		z-index:999999;\
		overflow:auto;\
	";
	document.body.appendChild(sidebar);
}

/*Handle requests from background.html*/
function handleRequest(
	//The object data with the request params
	request, 
	//These last two ones isn't important for this example, if you want know more about it visit: http://code.google.com/chrome/extensions/messaging.html
	sender, sendResponse
	) {
	if (request.callFunction == "toggleSidebar")
		toggleSidebar();
}

chrome.extension.onRequest.addListener(handleRequest);

var sidebarOpen = false;
function toggleSidebar() {
	if(sidebarOpen) {
		var el = document.getElementById('mySidebar');
		el.parentNode.removeChild(el);
		sidebarOpen = false;
		currentStack = null;
	}
	else {
		// send the current Stack Overflow url to the server
		socket.send("SO Link: " + window.location.href);

		// TODO: send all answer post ids to the server, finish this later after handling a single post
		/* var answers = $('.answer');
		var ids = "";
		if(answers.length == 0) {
			alert("Found no Stack Overflow answer posts");
		}
		for(var i = 0; i < answers.length; i++) {
			ids += answers[i].getAttribute("data-answerid") + ",";
		}
		socket.send("SO Answer Post Ids: " + ids; */
	}
}

