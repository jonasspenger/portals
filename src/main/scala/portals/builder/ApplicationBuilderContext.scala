package portals

/** Application Builder Context. */
class ApplicationBuilderContext(_path: String):
  var app: Application = Application(path = _path)

  def addToContext(e: AST): Unit = e match
    case x: Workflow[_, _] =>
      app = app.copy(workflows = app.workflows :+ x)
    case x: AtomicGenerator[_] =>
      app = app.copy(generators = app.generators :+ x)
    case x: AtomicStream[_] =>
      app = app.copy(streams = app.streams :+ x)
    case x: AtomicSequencer[_] =>
      app = app.copy(sequencers = app.sequencers :+ x)
    case x: AtomicSplitter[_] =>
      app = app.copy(splitters = app.splitters :+ x)
    case x: AtomicConnection[_] =>
      app = app.copy(connections = app.connections :+ x)
    case x: AtomicPortal[_, _] =>
      app = app.copy(portals = app.portals :+ x)
    case x: ExtAtomicStreamRef[_] =>
      app = app.copy(externalStreams = app.externalStreams :+ x)
    case x: ExtAtomicSequencerRef[_] =>
      app = app.copy(externalSequencers = app.externalSequencers :+ x)
    case x: ExtAtomicPortalRef[_, _] =>
      app = app.copy(externalPortals = app.externalPortals :+ x)
    case x: Application => ???
    case _ => ???

  private var _next_id: Int = 0
  def next_id(): String =
    _next_id = _next_id + 1
    "$" + _next_id.toString

  def name_or_id(name: String): String = if name == null then this.next_id() else name

  // TODO: get rid of this anomaly
  var _workflowBuilders: List[WorkflowBuilder[_, _]] = List.empty
  def freeze(): Unit =
    _workflowBuilders.foreach { _.complete() }
