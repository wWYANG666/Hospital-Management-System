export type ScheduleStatus =
  | "ACTIVE"
  | "LEAVE_PENDING"
  | "DISABLED"
  | "ENDED"
  | "UNKNOWN";
type ScheduleRow = {
  workDate?: unknown;
  workTime?: unknown;
  status?: unknown;
  displayStatus?: unknown;
};
const labels: Record<ScheduleStatus, string> = {
  ACTIVE: "启用中",
  LEAVE_PENDING: "请假审批中",
  DISABLED: "已停用",
  ENDED: "已结束",
  UNKNOWN: "未设置",
};

export function scheduleState(schedule: ScheduleRow): ScheduleStatus {
  const state = String(schedule.displayStatus || "") as ScheduleStatus;
  if (state in labels) return state;
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date());
  const part = (type: string) =>
    parts.find((item) => item.type === type)?.value || "";
  const today = `${part("year")}-${part("month")}-${part("day")}`;
  const date = String(schedule.workDate || "").slice(0, 10);
  if (!date) return "UNKNOWN";
  const ends: Record<string, string> = {
    MORNING: "12:00",
    AFTERNOON: "17:00",
    EVENING: "21:30",
  };
  const end = ends[String(schedule.workTime)];
  if (
    date < today ||
    (date === today && end && `${part("hour")}:${part("minute")}` >= end)
  )
    return "ENDED";
  return Number(schedule.status) === 1
    ? "ACTIVE"
    : Number(schedule.status) === 2
      ? "LEAVE_PENDING"
      : schedule.status != null && Number(schedule.status) === 0
        ? "DISABLED"
        : "UNKNOWN";
}

export function scheduleLabel(value: ScheduleRow | ScheduleStatus): string {
  return (
    labels[typeof value === "string" ? value : scheduleState(value)] ||
    labels.UNKNOWN
  );
}

export function scheduleTone(schedule: ScheduleRow): string {
  const state = scheduleState(schedule);
  return state === "ACTIVE"
    ? "status-success"
    : state === "LEAVE_PENDING"
      ? "status-warning"
      : "status-muted";
}
