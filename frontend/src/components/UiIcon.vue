<script setup lang="ts">
import { computed } from "vue";
import {
  Activity,
  ArrowRight,
  BedDouble,
  Building2,
  CalendarDays,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Clock,
  CreditCard,
  Download,
  FileText,
  Heart,
  Hospital,
  Info,
  LogOut,
  Menu,
  Pill,
  Plus,
  Search,
  Settings,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  UserRound,
  UsersRound,
  X,
  CircleAlert,
  Bell,
  RefreshCw,
  Trash2,
} from "@lucide/vue";
const props = withDefaults(
  defineProps<{ name?: string; size?: number | string }>(),
  { name: "activity", size: 20 },
);
const icons: Record<string, any> = {
  activity: Activity,
  "arrow-right": ArrowRight,
  bed: BedDouble,
  building: Building2,
  calendar: CalendarDays,
  check: Check,
  "chevron-down": ChevronDown,
  "chevron-left": ChevronLeft,
  "chevron-right": ChevronRight,
  clipboard: ClipboardList,
  clock: Clock,
  "credit-card": CreditCard,
  wallet: CreditCard,
  download: Download,
  file: FileText,
  heart: Heart,
  hospital: Hospital,
  info: Info,
  logout: LogOut,
  menu: Menu,
  pill: Pill,
  plus: Plus,
  search: Search,
  settings: Settings,
  shield: ShieldCheck,
  sparkles: Sparkles,
  stethoscope: Stethoscope,
  user: UserRound,
  users: UsersRound,
  close: X,
  alert: CircleAlert,
  bell: Bell,
  refresh: RefreshCw,
  trash: Trash2,
};
const icon = computed(() => icons[props.name] || Activity);
</script>
<template>
  <component
    :is="icon"
    :size="size"
    :stroke-width="1.7"
    class="ui-icon"
    aria-hidden="true"
  />
</template>
