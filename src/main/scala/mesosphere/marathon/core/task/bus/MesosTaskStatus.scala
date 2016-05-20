package mesosphere.marathon.core.task.bus

import org.apache.mesos.Protos.TaskState._
import org.apache.mesos.Protos.TaskStatus
import org.apache.mesos.Protos.TaskStatus.Reason._

object MesosTaskStatus {

  // If we're disconnected at the time of a TASK_LOST event, we will only get the update during
  // a reconciliation. In that case, the specific reason will be shadowed by REASON_RECONCILIATION.
  // Since we don't know the original reason, we need to assume that the task might come back.
  val MightComeBack: Set[TaskStatus.Reason] = Set(
    REASON_RECONCILIATION,
    REASON_SLAVE_DISCONNECTED,
    REASON_SLAVE_REMOVED,
    REASON_SLAVE_RESTARTED,
    REASON_SLAVE_UNKNOWN)

  val WontComeBack: Set[TaskStatus.Reason] = TaskStatus.Reason.values().toSet.diff(MightComeBack)

  object Terminal {
    def unapply(taskStatus: TaskStatus): Option[TaskStatus] = taskStatus.getState match {
      case TASK_LOST if !MightComeBack(taskStatus.getReason) => Some(taskStatus)
      case TASK_ERROR | TASK_FAILED | TASK_KILLED | TASK_FINISHED => Some(taskStatus)
      case _ => None
    }
  }

  object TemporarilyUnreachable {
    def unapply(taskStatus: TaskStatus): Option[TaskStatus] = {
      if (taskStatus.getState == TASK_LOST && MightComeBack(taskStatus.getReason)) Some(taskStatus)
      else None
    }
  }

  object Running {
    def unapply(taskStatus: TaskStatus): Option[TaskStatus] = taskStatus.getState match {
      case TASK_RUNNING => Some(taskStatus)
      case _            => None
    }
  }
}