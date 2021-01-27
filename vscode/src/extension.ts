'use strict';
import * as net from 'net';
import * as path from 'path';
import { workspace, ExtensionContext, window, TreeDataProvider,EventEmitter, Event, TreeItem, ProviderResult,Uri,commands, TreeItemCollapsibleState, Location,TextEditorRevealType, DecorationOptions, TreeView,  Selection, Position, CodeAction } from 'vscode';
import { LanguageClient, LanguageClientOptions, ServerOptions, StreamInfo, PublishDiagnosticsParams, Diagnostic, DiagnosticSeverity, Command, VersionedTextDocumentIdentifier, Range} from 'vscode-languageclient';


// this method is called when your extension is activated
// your extension is activated the very first time the command is executed
export async function activate(context: ExtensionContext) {
		// Startup options for the language server
	//const lspTransport = workspace.getConfiguration().get("taintbench.lspTransport", "socket")
	const lspTransport = workspace.getConfiguration().get("taintbench.lspTransport", "stdio")

    let script = 'java';
    let args = ['-jar',context.asAbsolutePath(path.join('TB-Viewer-0.0.2-SNAPSHOT.jar'))];
	
	const serverOptionsStdio = {
		run : { command: script, args: args },
        debug: { command: script, args: args} //, options: { env: createDebugEnv() }
	}

    const serverOptionsSocket = () => {
		const socket = net.connect({ port: 5007 })
		const result: StreamInfo = {
			writer: socket,
			reader: socket
		}
		return new Promise<StreamInfo>((resolve) => {
			socket.on("connect", () => resolve(result))
			socket.on("error", _ =>
				window.showErrorMessage(
					"Failed to connect to TaintBench language server. Make sure that the language server is running " +
					"-or- configure the extension to connect via standard IO."))
		})
	}
	
	const serverOptions: ServerOptions =
		(lspTransport === "stdio") ? serverOptionsStdio : (lspTransport === "socket") ? serverOptionsSocket : null
		//(lspTransport === "socket") ? serverOptionsSocket : (lspTransport === "stdio") ? serverOptionsStdio : null
   
	let clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'java' }],
        synchronize: {
            configurationSection: 'java',
            fileEvents: [ workspace.createFileSystemWatcher('**/*.java') ]
        }
    };
	
	// Register commands
	commands.registerCommand("taintbench.goto", async (args: TreeViewNode) => {
		try {
			const location = args.command.arguments[0].location;
			const fileUri = location.uri;
			const doc = await workspace.openTextDocument(fileUri)
			const editor = await window.showTextDocument(doc, {
					preserveFocus: true,
					preview: false
				})
			const deco = window.createTextEditorDecorationType({
				textDecoration: 'underline'
			});
			const ops  = [];
			ops.push(location.range);
			const start=location.range.start;
			const end= location.range.end; 
			window.activeTextEditor.selection = new Selection(new Position(start.line, start.character), new Position(end.line, end.character));
			editor.setDecorations(deco, ops);
			editor.revealRange(location.range, TextEditorRevealType.InCenter);
		} catch (e) {
			window.showErrorMessage(e)
		}
	});

	// Setup taintbench tree views
	let groundtruthProvider = new SimpleTreeDataProvider();
	let aqlresultsProvider = new SimpleTreeDataProvider();
	let matchedresultsProvider = new SimpleTreeDataProvider();
	let unmatchedresultsProvider = new SimpleTreeDataProvider();

	window.registerTreeDataProvider('taintbench.groundtruth', groundtruthProvider);
	window.registerTreeDataProvider('taintbench.aqlresults', aqlresultsProvider);
	window.registerTreeDataProvider('taintbench.matchedresults', matchedresultsProvider);
	window.registerTreeDataProvider('taintbench.unmatchedresults', unmatchedresultsProvider);

    // Create the language client and start the client.
    let client : LanguageClient = new LanguageClient('TaintBench','TaintBench', serverOptions, clientOptions);
    client.start();
	await client.onReady();
	client.onNotification("taintbench/groundtruth",handleTreeDataNotification(groundtruthProvider,"taint.groundtruth", true));
    client.onNotification("taintbench/aqlresults",handleTreeDataNotification(aqlresultsProvider,"taint.aqlresults", false));
    client.onNotification("taintbench/matchedresults",handleTreeDataNotification(matchedresultsProvider,"taint.matchedresults", false));
	client.onNotification("taintbench/unmatchedresults",handleTreeDataNotification(unmatchedresultsProvider,"taint.unmatchedresults", false));
	
}


export class TreeViewNode extends TreeItem {
	children: TreeViewNode[] = new Array();
}

export class SimpleTreeDataProvider implements TreeDataProvider<TreeViewNode> {
	emitter = new EventEmitter<TreeViewNode>();
	onDidChangeTreeData?: Event<TreeViewNode> = this.emitter.event;
	rootItems: Array<TreeViewNode> = new Array();
	positiveFlows: Array<TreeViewNode>= new Array();
	negativeFlows: Array<TreeViewNode>= new Array();
	flows: Array<TreeViewNode>= new Array();
	sources: Array<TreeViewNode>= new Array();
	sinks: Array<TreeViewNode>= new Array();

	sourceStrs: Array<string>= new Array();
	sinkStrs: Array<string>= new Array();

	update(uri: string, diagnostics: Diagnostic[],viewId: String, seperation: boolean) {
		var summaryPositive = null;
		var summaryNegative = null;
		var summary=null;
		var source = null;
		var sink = null;
		if(this.rootItems.length === 0 )
		{
			if(seperation)
			{
				summaryPositive=new TreeViewNode("");	
				this.rootItems.push(summaryPositive);
				summaryNegative=new TreeViewNode("");
				this.rootItems.push(summaryNegative);
			}
			else
			{
				summary=new TreeViewNode("");
				this.rootItems.push(summary);
			}
			source=new TreeViewNode("Sources: ");
			this.rootItems.push(source);
			sink =new TreeViewNode("Sinks: ");
			this.rootItems.push(sink);
		}
		else
		{
			if(seperation)
			{

				summaryPositive = this.rootItems[0];
				summaryNegative =this.rootItems[1];
				source = this.rootItems[2];
				sink =this.rootItems[3];
			}
			else
			{
				summary = this.rootItems[0];
				source = this.rootItems[1];
				sink =this.rootItems[2];
			}
		}
		
		for(var i =0; i < diagnostics.length; i++)
		{
			let d = diagnostics[i]; 
			let flowNode = this.convert(uri, d);
			if(seperation)
			{
			if(d.severity === DiagnosticSeverity.Error)
				this.positiveFlows.push(flowNode);
			else if(d.severity === DiagnosticSeverity.Information)
				this.negativeFlows.push(flowNode);
			}else
			{
				this.flows.push(flowNode);
			}
		}
		if(seperation)
		{
			//sort the flows according to their IDs
			this.positiveFlows.sort((n1,n2) =>
			{
				var id1=parseInt(n1.label.split(".")[0]);
				var id2=parseInt(n2.label.split(".")[0]);
				return id1-id2;
			}
			);
		
			this.negativeFlows.sort((n1,n2) =>
			{
				var id1=parseInt(n1.label.split(".")[0]);
				var id2=parseInt(n2.label.split(".")[0]);
				return id1-id2;
			}
			);
		}
		else
		{
			this.flows.sort((n1,n2) =>
			{
				var id1=parseInt(n1.label.split(".")[0]);
				var id2=parseInt(n2.label.split(".")[0]);
				return id1-id2;
			}
			);
		}

		if(seperation)
		{
			summaryPositive.label="Positive Flows: "+this.positiveFlows.length;
			summaryPositive.children=this.positiveFlows;
			summaryPositive.iconPath = path.join(__filename, '..','..','media/summary.svg');
			summaryPositive.collapsibleState=TreeItemCollapsibleState.Expanded;

			summaryNegative.label="Negative Flows: "+this.negativeFlows.length;
			summaryNegative.children=this.negativeFlows;
			summaryNegative.iconPath = path.join(__filename, '..','..','media/summary.svg');
			summaryNegative.collapsibleState=TreeItemCollapsibleState.Expanded;
		}
		else
		{
			summary.label="Detected Flows: "+this.flows.length;
			summary.children=this.flows;
			summary.iconPath = path.join(__filename, '..','..','media/summary.svg');
			summary.collapsibleState=TreeItemCollapsibleState.Expanded;

		}

		source.label="Sources: "+this.sources.length;
		source.children=this.sources;
		source.iconPath = path.join(__filename, '..','..','media/summary.svg');
		source.collapsibleState=TreeItemCollapsibleState.Collapsed;

		sink.label="Sinks: "+this.sinks.length;
		sink.children=this.sinks;
		sink.iconPath = path.join(__filename, '..','..','media/summary.svg');
		sink.collapsibleState=TreeItemCollapsibleState.Collapsed;

		this.emitter.fire()
	}
	
	convert(uri: string, d : Diagnostic)
	{	
		var label = d.message;
		let node : TreeViewNode = new TreeViewNode(label);
		node.collapsibleState = TreeItemCollapsibleState.Collapsed;
		node.tooltip = d.message;
		node.iconPath = path.join(__filename, '..','..','media/warning.svg');
		node.resourceUri= Uri.parse(uri);
		node.command =  {
			command: 'taintbech.goto',
			title: 'goto',
			arguments: [
			{
				location: {
					uri: node.resourceUri,
					range: d.range
				}
			}
			]
		};
		node.children=new Array();

		//extract sink from aql result
		if(d.severity == DiagnosticSeverity.Warning)
		{
			var sinkInfo ="SINK: "+d.message.split("Detected taint flow to ")[1];
			let aqlSink : TreeViewNode = new TreeViewNode(sinkInfo);
			aqlSink.collapsibleState=TreeItemCollapsibleState.None;
			aqlSink.tooltip=sinkInfo;
			aqlSink.resourceUri=node.resourceUri;
			aqlSink.iconPath = path.join(__filename, '..','..','media/sink.svg');
			aqlSink.command =  {
				command: 'taintbech.goto',
				title: 'goto',
				arguments: [
				{
					location: {
						uri: aqlSink.resourceUri,
						range: d.range
					}
				}
				]
			}
			d.range.start.line+
			d.range.start.character+
			d.range.end.line+
			d.range.start.character;
		}

		// create nodes from related information. 
		for(var i =0; i<d.relatedInformation.length; i++)
		{
			let related = d.relatedInformation[i];
			let child : TreeViewNode = new TreeViewNode(related.message);
			child.collapsibleState=TreeItemCollapsibleState.None;
			child.tooltip=related.message;			
			child.resourceUri=Uri.parse(related.location.uri);
			child.command =  {
				command: 'taintbech.goto',
				title: 'goto',
				arguments: [
				{
					location: {
						uri: child.resourceUri,
						range: related.location.range
					}
				}
				]
			};
			

			if(related.message.startsWith("SOURCE"))
			{
				child.iconPath = path.join(__filename, '..','..','media/source.svg');
				var sourceStr = child.label+child.resourceUri+
				related.location.range.start.line+
				related.location.range.start.character+
				related.location.range.end.line+
				related.location.range.end.character;
				if(!this.sourceStrs.includes(sourceStr))
				{
					this.sourceStrs.push(sourceStr);
					this.sources.push(child);
				}
			}else if(related.message.startsWith("SINK"))
			{
				child.iconPath = path.join(__filename, '..','..','media/sink.svg');
				var sinkStr = child.label+child.resourceUri+
				related.location.range.start.line+
				related.location.range.start.character+
				related.location.range.end.line+
				related.location.range.end.character;
				if(!this.sinkStrs.includes(sinkStr))
				{
					this.sinkStrs.push(sinkStr);
					this.sinks.push(child);
				}
			}else
			{
				child.iconPath = path.join(__filename, '..','..','media/info.svg');
			}
			node.children.push(child);
		}
		return node;
	}
	
	getTreeItem(element: TreeViewNode): TreeItem | Thenable<TreeItem> {
		if (typeof element.resourceUri === 'string')
			element.resourceUri = Uri.parse(element.resourceUri)
		if (typeof element.iconPath === 'string' && element.iconPath.startsWith("~"))
			element.iconPath = path.join(__filename, "..", "..", "..", element.iconPath.substr(1))
		return element
	}

	getChildren(element?: TreeViewNode): ProviderResult<TreeViewNode[]> {
		if (element)
			return element.children
		return this.rootItems
	}

	getParent(element: TreeViewNode): ProviderResult<TreeViewNode> {
		function* allElements(parent: TreeViewNode, children: TreeViewNode[]): IterableIterator<{ parent: TreeViewNode, child: TreeViewNode }> {
			for (const child of children) {
				yield { parent, child }
				yield* allElements(child, child.children)
			}
		}

		for (const o of allElements(null, this.rootItems))
			if (o.child === element)
				return o.parent
	}
}

export function handleTreeDataNotification(dataProvider:  SimpleTreeDataProvider, viewId: string, seperation: boolean) {
	return (args: PublishDiagnosticsParams) => {
		if (dataProvider)
			dataProvider.update(args.uri, args.diagnostics, viewId, seperation);
	}
}
