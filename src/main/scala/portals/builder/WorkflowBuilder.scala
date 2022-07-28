package portals

import scala.annotation.targetName

trait WorkflowBuilder[T, U]:
  private[portals] def complete(): Unit

  def freeze(): Workflow[T, U]

  def source[TT >: T <: T](ref: AtomicStreamRefKind[T], name: String = null): FlowBuilder[T, U, TT, TT]

  // check if workflow is well-formed
  def check(): Boolean
end WorkflowBuilder // trait

object WorkflowBuilder:
  def apply[T, U](name: String)(using bctx: ApplicationBuilderContext): WorkflowBuilder[T, U] =
    val _path = bctx.app.path + "/workflows/" + name
    val _name = name
    given wbctx: WorkflowBuilderContext[T, U] = new WorkflowBuilderContext[T, U](_path = _path, _name = _name)
    new WorkflowBuilderImpl[T, U]()
end WorkflowBuilder // object
