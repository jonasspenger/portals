package pods.workflows

private[pods] trait ExecutionContext:
  def execute[T, U](opSpec: OperatorSpec[T, U]): OpRef[T, U]
  def shutdown(): Unit
end ExecutionContext