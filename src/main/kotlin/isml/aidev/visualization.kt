import ai.libs.jaicore.graphvisualizer.events.graph.bus.HandleAlgorithmEventException
import ai.libs.jaicore.graphvisualizer.events.gui.GUIEvent
import ai.libs.jaicore.graphvisualizer.events.recorder.property.AlgorithmEventPropertyComputer
import ai.libs.jaicore.graphvisualizer.plugin.ASimpleMVCPlugin
import ai.libs.jaicore.graphvisualizer.plugin.ASimpleMVCPluginController
import ai.libs.jaicore.graphvisualizer.plugin.ASimpleMVCPluginModel
import ai.libs.jaicore.graphvisualizer.plugin.ASimpleMVCPluginView
import ai.libs.jaicore.graphvisualizer.plugin.graphview.NodeClickedEvent
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeDisplayInfoAlgorithmEventPropertyComputer
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfo
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoAlgorithmEventPropertyComputer
import javafx.application.Platform
import javafx.scene.layout.StackPane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import org.api4.java.algorithm.events.serializable.IPropertyProcessedAlgorithmEvent

class NodeInfoGUIPlugin(title: String?) :
    ASimpleMVCPlugin<NodeInfoGUIPluginModel?, NodeInfoGUIPluginView?, NodeInfoGUIPluginController?>(title) {
    override fun getPropertyComputers(): MutableCollection<AlgorithmEventPropertyComputer> {
        TODO("Not yet implemented")
    }
}

class NodeInfoGUIPluginModel :
    ASimpleMVCPluginModel<NodeInfoGUIPluginView?, NodeInfoGUIPluginController?>() {
    private val nodeIdToNodeInfoMap: MutableMap<String?, String>
    var currentlySelectedNode: String? = null
        set(currentlySelectedNode) {
            field = currentlySelectedNode
            view!!.update()
        }

    fun addNodeIdToNodeInfoMapping(nodeId: String?, nodeInfo: String) {
        nodeIdToNodeInfoMap[nodeId] = nodeInfo
    }

    fun getNodeInfoForNodeId(nodeId: String?): String? {
        return nodeIdToNodeInfoMap[nodeId]
    }

    val nodeInfoForCurrentlySelectedNode: String?
        get() = getNodeInfoForNodeId(currentlySelectedNode)

    override fun clear() {
        /* ignore this */
    }

    init {
        nodeIdToNodeInfoMap = HashMap()
    }
}


class NodeInfoGUIPluginView(model: NodeInfoGUIPluginModel?) :
    ASimpleMVCPluginView<NodeInfoGUIPluginModel?, NodeInfoGUIPluginController?, StackPane?>(model, StackPane()) {
    private var webViewEngine: WebEngine? = null
    override fun update() {
        val nodeInfoOfCurrentlySelectedNode = model!!.nodeInfoForCurrentlySelectedNode
        Platform.runLater { webViewEngine!!.loadContent(nodeInfoOfCurrentlySelectedNode) }
    }

    override fun clear() {
        /* don't do anything */
    }

    init {
        Platform.runLater {
            val view = WebView()
            val node = node
            node!!.children.add(view)
            webViewEngine = view.engine
        }
    }
}


class NodeInfoGUIPluginController(model: NodeInfoGUIPluginModel?, view: NodeInfoGUIPluginView?) :
    ASimpleMVCPluginController<NodeInfoGUIPluginModel?, NodeInfoGUIPluginView?>(model, view) {
    @Throws(HandleAlgorithmEventException::class)
    override fun handleAlgorithmEventInternally(algorithmEvent: IPropertyProcessedAlgorithmEvent) {
        val rawNodeDisplayInfoProperty =
            algorithmEvent.getProperty(NodeDisplayInfoAlgorithmEventPropertyComputer.NODE_DISPLAY_INFO_PROPERTY_NAME)
        val rawNodeInfoProperty =
            algorithmEvent.getProperty(NodeInfoAlgorithmEventPropertyComputer.NODE_INFO_PROPERTY_NAME)
        if (rawNodeDisplayInfoProperty != null && rawNodeInfoProperty != null) {
            val nodeInfo = rawNodeInfoProperty as NodeInfo
            val nodeInfoText = rawNodeDisplayInfoProperty as String
            model!!.addNodeIdToNodeInfoMapping(nodeInfo.mainNodeId, nodeInfoText)
        }
    }

    override fun handleGUIEvent(guiEvent: GUIEvent) {
        if (NodeClickedEvent::class.java.isInstance(guiEvent)) {
            val nodeClickedEvent = guiEvent as NodeClickedEvent
            val searchGraphNodeCorrespondingToClickedViewGraphNode = nodeClickedEvent.searchGraphNode
            model!!.currentlySelectedNode = searchGraphNodeCorrespondingToClickedViewGraphNode
        }
    }
}
