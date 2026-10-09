package xmu.edu.yiyuan.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Examination;
import xmu.edu.yiyuan.entity.Medicine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AiSuggestionService {

    /** 从病历拼接串中剔除误入的 AI 建议块，避免「既往病史」里再嵌一整段模板 */
    private static final Pattern DIAGNOSIS_AI_SECTION =
            Pattern.compile("【(?:主诉概括|可能相关方向|结构化上下文|既往/当前诊断参考|用药注意|检查/检验建议|随访与宣教)[^】]*】");

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private AiGenerationAuditService aiGenerationAuditService;

    @Value("${ai.qwen.enabled:false}")
    private boolean qwenEnabled;

    @Value("${ai.qwen.api-key:${DASHSCOPE_API_KEY:}}")
    private String qwenApiKey;

    @Value("${ai.qwen.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String qwenEndpoint;

    @Value("${ai.qwen.model:qwen-plus}")
    private String qwenModel;

    @Value("${ai.qwen.timeout-seconds:8}")
    private int qwenTimeoutSeconds;

    private DiagnosisRules diagnosisRules = new DiagnosisRules();
    private TriageRules triageRules = new TriageRules();
    private ReportTemplates reportTemplates = new ReportTemplates();

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @PostConstruct
    public void loadRules() {
        this.diagnosisRules = loadDiagnosisRules("classpath:ai-rules/diagnosis-rules.json");
        this.triageRules = loadTriageRules("classpath:ai-rules/triage-keywords.json");
        this.reportTemplates = loadReportTemplates("classpath:ai-rules/report-templates.json");
    }

    private Map<String, Object> loadJsonAsMap(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                return Map.of();
            }
            String json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonParser parser = JsonParserFactory.getJsonParser();
            return parser.parseMap(json);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private DiagnosisRules loadDiagnosisRules(String location) {
        DiagnosisRules rules = new DiagnosisRules();
        Map<String, Object> root = loadJsonAsMap(location);
        Object directionsObj = root.get("symptomDirections");
        if (directionsObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) continue;
                SymptomDirection d = new SymptomDirection();
                Object kws = m.get("keywords");
                if (kws instanceof List<?> kwList) {
                    for (Object kw : kwList) {
                        if (kw != null) d.keywords.add(String.valueOf(kw));
                    }
                }
                d.advice = stringOf(m.get("advice"));
                if (!d.keywords.isEmpty() && d.advice != null) {
                    rules.symptomDirections.add(d);
                }
            }
        }
        Object ctxObj = root.get("contextFragments");
        if (ctxObj instanceof Map<?, ?> m) {
            rules.contextFragments.ageElderly = stringOf(m.get("ageElderly"));
            rules.contextFragments.ageChild = stringOf(m.get("ageChild"));
            rules.contextFragments.genderMale = stringOf(m.get("genderMale"));
            rules.contextFragments.genderFemale = stringOf(m.get("genderFemale"));
            rules.contextFragments.historyPrefix = stringOf(m.get("historyPrefix"));
            rules.contextFragments.departmentPrefix = stringOf(m.get("departmentPrefix"));
        }
        Object fixedObj = root.get("fixedFragments");
        if (fixedObj instanceof Map<?, ?> m) {
            rules.fixedFragments.symptomSummaryPrefix = stringOf(m.get("symptomSummaryPrefix"));
            rules.fixedFragments.existingDiagnosisPrefix = stringOf(m.get("existingDiagnosisPrefix"));
            rules.fixedFragments.medicinePrefix = stringOf(m.get("medicinePrefix"));
            rules.fixedFragments.examPrefix = stringOf(m.get("examPrefix"));
            rules.fixedFragments.followup = stringOf(m.get("followup"));
        }
        return rules;
    }

    private TriageRules loadTriageRules(String location) {
        TriageRules rules = new TriageRules();
        Map<String, Object> root = loadJsonAsMap(location);
        Object defaultObj = root.get("default");
        if (defaultObj instanceof Map<?, ?> m) {
            rules.defaultRule.department = stringOf(m.get("department"));
            rules.defaultRule.urgency = stringOf(m.get("urgency"));
            rules.defaultRule.advice = stringOf(m.get("advice"));
        }
        Object listObj = root.get("rules");
        if (listObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) continue;
                TriageRule r = new TriageRule();
                r.department = stringOf(m.get("department"));
                r.urgency = stringOf(m.get("urgency"));
                r.advice = stringOf(m.get("advice"));
                Object kws = m.get("keywords");
                if (kws instanceof List<?> kwList) {
                    for (Object kw : kwList) {
                        if (kw != null) r.keywords.add(String.valueOf(kw));
                    }
                }
                rules.rules.add(r);
            }
        }
        return rules;
    }

    private ReportTemplates loadReportTemplates(String location) {
        ReportTemplates templates = new ReportTemplates();
        Map<String, Object> root = loadJsonAsMap(location);
        fillReportTemplateBlock(root.get("bloodTemplate"), templates.bloodTemplate);
        fillReportTemplateBlock(root.get("imagingTemplate"), templates.imagingTemplate);
        fillReportTemplateBlock(root.get("generalTemplate"), templates.generalTemplate);
        templates.clinicalNoteTemplate = stringOf(root.get("clinicalNoteTemplate"));
        templates.clinicalNoteFallback = stringOf(root.get("clinicalNoteFallback"));
        return templates;
    }

    private void fillReportTemplateBlock(Object source, ReportTemplateBlock target) {
        if (!(source instanceof Map<?, ?> m) || target == null) {
            return;
        }
        target.findingDefault = stringOf(m.get("findingDefault"));
        target.impression = stringOf(m.get("impression"));
    }

    private String stringOf(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 供病历诊断拼接处过滤：明显为整段 AI 草稿或过长文本时不写入 historySummary。
     */
    public boolean isUnsuitableDiagnosisForAiHistory(String diagnosis) {
        if (diagnosis == null) {
            return true;
        }
        String t = diagnosis.trim();
        if (t.isEmpty()) {
            return true;
        }
        if (t.contains("【") && t.contains("】")) {
            return true;
        }
        if (t.length() > 100) {
            return true;
        }
        return t.contains("建议结合慢病管理与本次症状变化综合判断")
                || t.contains("建议优先遵循本科室诊疗路径并结合多学科协作");
    }

    private String sanitizeDiagnosisHistorySummary(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.replace('\r', ' ').replace('\n', ' ');
        String prev;
        do {
            prev = t;
            t = DIAGNOSIS_AI_SECTION.matcher(t).replaceAll(" ");
        } while (!t.equals(prev));
        t = t.replace("【结构化上下文】", " ");
        t = t.replaceAll("当前接诊科室：[^。；]+。", " ");
        t = t.replace("建议结合慢病管理与本次症状变化综合判断。", "");
        t = t.replace("建议优先遵循本科室诊疗路径并结合多学科协作。", "");
        t = t.replaceAll("\\s+", " ").trim();
        if (t.length() > 120) {
            t = t.substring(0, 120) + "…";
        }
        return t;
    }

    private String stripBoilerplateFromClinicalLine(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String t = sanitizeDiagnosisHistorySummary(line);
        return t.trim();
    }

    private String normalizeChinesePeriods(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("。。", "。").replaceAll("(。){3,}", "。");
    }

    public String buildDiagnosisSuggestion(String symptoms,
                                           String existingDiagnosis,
                                           List<Medicine> medicines,
                                           List<Examination> examinations) {
        return buildDiagnosisSuggestion(symptoms, existingDiagnosis, medicines, examinations, null, "unknown", "doctor.diagnosis");
    }

    public String buildDiagnosisSuggestion(String symptoms,
                                           String existingDiagnosis,
                                           List<Medicine> medicines,
                                           List<Examination> examinations,
                                           DiagnosisContext context,
                                           String who,
                                           String where) {
        StringBuilder sb = new StringBuilder();
        String cleanedSymptoms = stripBoilerplateFromClinicalLine(safeTrim(symptoms));
        String cleanedDx = stripBoilerplateFromClinicalLine(safeTrim(existingDiagnosis));

        if (!cleanedSymptoms.isEmpty()) {
            sb.append(String.format(
                    safeTemplate(diagnosisRules.fixedFragments.symptomSummaryPrefix, "【主诉概括】患者主诉：%s；需结合体格检查及既往病史进一步评估。"),
                    cleanedSymptoms
            )).append("\n\n");
        }

        List<String> directionAdvices = new ArrayList<>();
        for (SymptomDirection d : safeList(diagnosisRules.symptomDirections)) {
            if (d == null || d.keywords == null || d.advice == null) {
                continue;
            }
            boolean matched = d.keywords.stream().anyMatch(k -> containsText(cleanedSymptoms, k));
            if (matched) {
                directionAdvices.add(d.advice);
            }
        }
        sb.append("【可能相关方向】");
        if (directionAdvices.isEmpty()) {
            sb.append("须结合查体与辅助检查明确病因。\n\n");
        } else {
            sb.append(String.join("；", directionAdvices)).append("。\n\n");
        }

        if (context != null) {
            appendContextBlock(sb, context);
        }

        // 避免与上文重复：与净化后的既往要点比对
        if (!cleanedDx.isEmpty()) {
            String histClean = context != null ? sanitizeDiagnosisHistorySummary(context.historySummary) : "";
            boolean dupInHistory = !histClean.isEmpty() && histClean.contains(cleanedDx);
            boolean dupAsSymptom = !cleanedSymptoms.isEmpty() && cleanedSymptoms.equals(cleanedDx);
            if (!dupInHistory && !dupAsSymptom) {
                sb.append(String.format(
                        safeTemplate(diagnosisRules.fixedFragments.existingDiagnosisPrefix, "【既往/当前诊断参考】当前记录中已有诊断描述：%s。在此基础上，可结合本次病情变化判断是否需要调整诊疗方案。"),
                        cleanedDx
                )).append("\n\n");
            }
        }

        String medStr = joinMedicineNames(medicines);
        if (!medStr.isEmpty()) {
            sb.append(String.format(
                    safeTemplate(diagnosisRules.fixedFragments.medicinePrefix, "【用药注意】本次拟使用或已选择药物：%s。请注意核对过敏史、肝肾功能及药物相互作用，必要时调整剂量或给药间隔；同时向患者说明用药方法及常见不良反应，嘱其如出现明显不适及时就诊。"),
                    medStr
            )).append("\n\n");
        }

        String examStr = joinExamNames(examinations);
        if (!examStr.isEmpty()) {
            sb.append(String.format(
                    safeTemplate(diagnosisRules.fixedFragments.examPrefix, "【检查/检验建议】已勾选或拟考虑的项目：%s。请结合患者具体情况判断检查必要性与优先级，避免重复或不必要检查；检查结果需与临床表现综合解读。"),
                    examStr
            )).append("\n\n");
        }

        sb.append(safeTemplate(
                diagnosisRules.fixedFragments.followup,
                "【随访与宣教】建议向患者说明复诊与危险信号，症状加重时及时就医。"
        ));

        String output = normalizeChinesePeriods(sb.toString());
        aiGenerationAuditService.log(
                who,
                where,
                Map.of(
                        "symptoms", cleanedSymptoms,
                        "existingDiagnosis", cleanedDx,
                        "medicineCount", medicines == null ? 0 : medicines.size(),
                        "examinationCount", examinations == null ? 0 : examinations.size(),
                        "context", context == null ? Map.of() : Map.of(
                                "age", context.age == null ? "" : context.age,
                                "gender", safeTrim(context.gender),
                                "historySummary", safeTrim(context.historySummary),
                                "department", safeTrim(context.department)
                        )
                ),
                output
        );
        return output;
    }

    public String buildReportSuggestion(Examination exam,
                                        String rawResult,
                                        String clinicalNote) {
        return buildReportSuggestion(exam, rawResult, clinicalNote, "unknown", "doctor.report");
    }

    public String buildReportSuggestion(Examination exam,
                                        String rawResult,
                                        String clinicalNote,
                                        String who,
                                        String where) {
        StringBuilder sb = new StringBuilder();

        String examName = exam != null && exam.getName() != null ? exam.getName() : "检查项目";
        String examType = exam != null ? safeTrim(exam.getType()) : "";
        String cleanedResult = safeTrim(rawResult);
        String cleanedClinicalNote = safeTrim(clinicalNote);

        sb.append("【检查项目】").append(examName);
        if (!examType.isBlank()) {
            sb.append("（").append(examType).append("）");
        }
        sb.append("。\n\n");

        ReportTemplateBlock block = pickReportTemplate(examName, examType);
        sb.append("【检查所见】")
                .append(!cleanedResult.isBlank() ? cleanedResult : safeTemplate(block.findingDefault, "检查结果待完善。"))
                .append("\n\n")
                .append("【诊断意见】")
                .append(safeTemplate(block.impression, "建议结合本次检查结果与临床表现进行综合判断，必要时复查或追加相关检查。"))
                .append("\n\n");

        if (!cleanedClinicalNote.isBlank()) {
            sb.append(String.format(
                    safeTemplate(reportTemplates.clinicalNoteTemplate, "【临床提示】当前记录中的临床信息：%s。报告解读时应结合患者症状、体征与既往史。"),
                    cleanedClinicalNote
            ));
        } else {
            sb.append(safeTemplate(reportTemplates.clinicalNoteFallback, "【临床提示】请结合患者主诉、体征及既往史对本报告进行综合解读。"));
        }
        String output = sb.toString();
        aiGenerationAuditService.log(
                who,
                where,
                Map.of(
                        "examName", examName,
                        "examType", examType,
                        "rawResult", cleanedResult,
                        "clinicalNote", cleanedClinicalNote
                ),
                output
        );
        return output;
    }

    public static class TriageResult {
        public String department;
        public String urgency;
        public String advice;
    }

    public TriageResult buildTriageSuggestion(String description) {
        return buildTriageSuggestion(description, "unknown", "patient.triage");
    }

    public TriageResult buildTriageSuggestion(String description, String who, String where) {
        String desc = description != null ? description.trim() : "";

        // 优先调用阿里 qwen-plus；失败时回退到本地关键词规则，保证系统可用性
        TriageResult modelResult = buildTriageSuggestionByQwen(desc, who, where);
        if (modelResult != null) {
            return modelResult;
        }

        TriageResult r = new TriageResult();
        if (desc.isEmpty()) {
            r.department = safeTemplate(triageRules.defaultRule.department, "全科/内科");
            r.urgency = safeTemplate(triageRules.defaultRule.urgency, "普通就诊");
            r.advice = safeTemplate(triageRules.defaultRule.advice, "建议先挂全科或内科，由首诊医生根据症状再决定是否转诊至专科。");
            aiGenerationAuditService.log(who, where, Map.of("description", desc), r.advice);
            return r;
        }

        String lower = desc.toLowerCase(Locale.ROOT);
        for (TriageRule rule : safeList(triageRules.rules)) {
            if (rule == null || rule.keywords == null || rule.keywords.isEmpty()) {
                continue;
            }
            boolean matched = rule.keywords.stream().anyMatch(k -> containsText(desc, k) || containsText(lower, k.toLowerCase(Locale.ROOT)));
            if (matched) {
                r.department = safeTemplate(rule.department, "全科/内科");
                r.urgency = safeTemplate(rule.urgency, "普通就诊");
                r.advice = safeTemplate(rule.advice, safeTemplate(triageRules.defaultRule.advice, "请结合线下医生评估。"));
                aiGenerationAuditService.log(who, where, Map.of("description", desc, "matchedKeywords", rule.keywords), r.advice);
                return r;
            }
        }

        r.department = safeTemplate(triageRules.defaultRule.department, "全科/内科");
        r.urgency = safeTemplate(triageRules.defaultRule.urgency, "普通就诊");
        r.advice = safeTemplate(triageRules.defaultRule.advice, "建议先挂全科或内科，由首诊医生根据症状再决定是否转诊至专科；如症状突然加重或出现明显不适，应及时就近就医。");
        aiGenerationAuditService.log(who, where, Map.of("description", desc, "matchedKeywords", List.of()), r.advice);
        return r;
    }

    private TriageResult buildTriageSuggestionByQwen(String description, String who, String where) {
        if (!qwenEnabled || qwenApiKey == null || qwenApiKey.isBlank()) {
            return null;
        }

        try {
            String userContent = description == null || description.isBlank()
                    ? "患者描述为空。请给出最稳妥的首诊建议。"
                    : ("患者症状描述：" + description);

            String payload = buildQwenTriagePayload(userContent);
            HttpRequest request = HttpRequest.newBuilder(URI.create(qwenEndpoint))
                    .timeout(Duration.ofSeconds(Math.max(3, qwenTimeoutSeconds)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + qwenApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            String body = response.body() == null ? "" : response.body();
            Map<String, Object> root = jsonParser.parseMap(body);
            String content = extractQwenContent(root);
            if (content == null || content.isBlank()) {
                return null;
            }

            String cleaned = stripMarkdownCodeFence(content);
            Map<String, Object> modelJson;
            try {
                modelJson = jsonParser.parseMap(cleaned);
            } catch (Exception parseEx) {
                return null;
            }

            String department = stringOf(modelJson.get("department"));
            String urgency = stringOf(modelJson.get("urgency"));
            String advice = stringOf(modelJson.get("advice"));
            if (department == null || urgency == null || advice == null) {
                return null;
            }

            TriageResult r = new TriageResult();
            r.department = department;
            r.urgency = urgency;
            r.advice = advice;
            aiGenerationAuditService.log(
                    who,
                    where,
                    Map.of("description", description == null ? "" : description, "provider", "aliyun-qwen-plus"),
                    "department=" + r.department + "; urgency=" + r.urgency + "; advice=" + r.advice
            );
            return r;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildQwenTriagePayload(String userContent) {
        String model = qwenModel == null || qwenModel.isBlank() ? "qwen-plus" : qwenModel.trim();
        String systemPrompt =
                "你是医院导诊助手。根据患者描述推荐就诊科室。"
                        + "必须仅输出一个 JSON 对象，不要输出任何额外文字。"
                        + "JSON 字段固定为 department、urgency、advice。"
                        + "urgency 只能是：普通就诊、尽快就诊、建议急诊。"
                        + "若存在急危重症风险，department 优先推荐“急诊科”。";
        return "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"temperature\":0.2,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + jsonEscape(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(userContent) + "\"}"
                + "]"
                + "}";
    }

    @SuppressWarnings("unchecked")
    private String extractQwenContent(Map<String, Object> root) {
        if (root == null) return null;
        Object choicesObj = root.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object messageObj = ((Map<String, Object>) firstMap).get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object contentObj = ((Map<String, Object>) messageMap).get("content");
        return contentObj == null ? null : String.valueOf(contentObj);
    }

    private String stripMarkdownCodeFence(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstLineEnd = t.indexOf('\n');
            if (firstLineEnd > 0) {
                t = t.substring(firstLineEnd + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3).trim();
            }
        }
        return t;
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public String buildAdminSummary(long todayAppointments,
                                    long inHospitalCount,
                                    long pendingHospitalizations,
                                    long occupiedBeds,
                                    long totalBeds,
                                    int lowStockCount) {
        return buildAdminSummary(todayAppointments, inHospitalCount, pendingHospitalizations, occupiedBeds, totalBeds, lowStockCount, "unknown", "admin.summary");
    }

    public String buildAdminSummary(long todayAppointments,
                                    long inHospitalCount,
                                    long pendingHospitalizations,
                                    long occupiedBeds,
                                    long totalBeds,
                                    int lowStockCount,
                                    String who,
                                    String where) {
        double bedRate = totalBeds == 0 ? 0.0 : occupiedBeds * 100.0 / totalBeds;
        StringBuilder sb = new StringBuilder();

        sb.append("【整体概览】");
        sb.append("今日门诊量约 ").append(todayAppointments).append(" 人，");
        sb.append("在院患者 ").append(inHospitalCount).append(" 人，");
        sb.append("床位使用率约 ").append(String.format("%.1f", bedRate)).append("%。");
        if (pendingHospitalizations > 0) {
            sb.append("当前有 ").append(pendingHospitalizations).append(" 份住院申请待审批，需关注审批节奏与床位周转。");
        } else {
            sb.append("当前暂无待审批住院申请。");
        }
        sb.append("\n\n");

        sb.append("【资源与风险点】");
        if (lowStockCount > 0) {
            sb.append("存在 ").append(lowStockCount).append(" 种低库存药品，建议药房尽快核对实际库存并安排补货；");
        } else {
            sb.append("药品库存整体平稳，暂无明显低库存预警；");
        }
        if (bedRate >= 90.0) {
            sb.append("床位资源已接近饱和，需重点协调出院及转科，避免影响急诊住院收治。");
        } else if (bedRate >= 70.0) {
            sb.append("床位使用率偏高，可适当关注重点病区的周转效率。");
        } else {
            sb.append("床位资源相对充裕，可根据业务量灵活调整排班与收治策略。");
        }
        sb.append("\n\n");

        sb.append("【管理建议】适时回顾近期门诊与住院量变化趋势，结合低库存药品与床位使用情况，");
        sb.append("对排班、药品采购及床位规划做必要微调，以保证诊疗秩序平稳运行。");

        String output = sb.toString();
        aiGenerationAuditService.log(
                who,
                where,
                Map.of(
                        "todayAppointments", todayAppointments,
                        "inHospitalCount", inHospitalCount,
                        "pendingHospitalizations", pendingHospitalizations,
                        "occupiedBeds", occupiedBeds,
                        "totalBeds", totalBeds,
                        "lowStockCount", lowStockCount
                ),
                output
        );
        return output;
    }

    private void appendContextBlock(StringBuilder sb, DiagnosisContext context) {
        List<String> lines = new ArrayList<>();
        if (context.age != null) {
            if (context.age >= 65) {
                lines.add(safeTemplate(diagnosisRules.contextFragments.ageElderly, "患者为老年人群，建议评估多病共存、跌倒风险及药物相互作用风险。"));
            } else if (context.age <= 14) {
                lines.add(safeTemplate(diagnosisRules.contextFragments.ageChild, "患者为儿童/青少年，建议关注体重相关用药剂量、脱水风险与监护指标。"));
            }
        }
        String gender = safeTrim(context.gender).toUpperCase(Locale.ROOT);
        if ("MALE".equals(gender) || "男".equals(gender)) {
            lines.add(safeTemplate(diagnosisRules.contextFragments.genderMale, "男性患者可结合吸烟、饮酒等危险因素进行分层评估。"));
        } else if ("FEMALE".equals(gender) || "女".equals(gender)) {
            lines.add(safeTemplate(diagnosisRules.contextFragments.genderFemale, "女性患者建议结合内分泌/生理周期等因素进行鉴别诊断。"));
        }
        if (!safeTrim(context.historySummary).isEmpty()) {
            String hist = sanitizeDiagnosisHistorySummary(context.historySummary);
            if (!hist.isEmpty()) {
                lines.add(String.format(
                        safeTemplate(diagnosisRules.contextFragments.historyPrefix, "既往要点：%s"),
                        hist
                ));
            }
        }
        if (!safeTrim(context.department).isEmpty()) {
            lines.add(String.format(
                    safeTemplate(diagnosisRules.contextFragments.departmentPrefix, "科室：%s"),
                    safeTrim(context.department)
            ));
        }
        if (!lines.isEmpty()) {
            sb.append("【结构化上下文】")
                    .append(String.join(" ", lines))
                    .append("\n\n");
        }
    }

    private ReportTemplateBlock pickReportTemplate(String examName, String examType) {
        String name = examName == null ? "" : examName;
        String type = examType == null ? "" : examType;
        if (type.contains("血")) {
            return reportTemplates.bloodTemplate;
        }
        if (type.contains("影像") || type.contains("CT") || name.contains("彩超")) {
            return reportTemplates.imagingTemplate;
        }
        return reportTemplates.generalTemplate;
    }

    private String joinMedicineNames(List<Medicine> medicines) {
        StringJoiner medNames = new StringJoiner("、");
        for (Medicine m : safeList(medicines)) {
            if (m != null && m.getName() != null) {
                medNames.add(m.getName());
            }
        }
        return medNames.toString();
    }

    private String joinExamNames(List<Examination> examinations) {
        StringJoiner examNames = new StringJoiner("、");
        for (Examination e : safeList(examinations)) {
            if (e != null && e.getName() != null) {
                examNames.add(e.getName());
            }
        }
        return examNames.toString();
    }

    private boolean containsText(String source, String target) {
        if (source == null || target == null || target.isBlank()) {
            return false;
        }
        return source.contains(target);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeTemplate(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    public static class DiagnosisContext {
        public Integer age;
        public String gender;
        public String historySummary;
        public String department;
    }

    public static class SymptomDirection {
        public List<String> keywords = new ArrayList<>();
        public String advice;
    }

    public static class DiagnosisContextFragments {
        public String ageElderly;
        public String ageChild;
        public String genderMale;
        public String genderFemale;
        public String historyPrefix;
        public String departmentPrefix;
    }

    public static class DiagnosisFixedFragments {
        public String symptomSummaryPrefix;
        public String existingDiagnosisPrefix;
        public String medicinePrefix;
        public String examPrefix;
        public String followup;
    }

    public static class DiagnosisRules {
        public List<SymptomDirection> symptomDirections = new ArrayList<>();
        public DiagnosisContextFragments contextFragments = new DiagnosisContextFragments();
        public DiagnosisFixedFragments fixedFragments = new DiagnosisFixedFragments();
    }

    public static class TriageRule {
        public String department;
        public String urgency;
        public String advice;
        public List<String> keywords = new ArrayList<>();
    }

    public static class TriageDefaultRule {
        public String department = "全科/内科";
        public String urgency = "普通就诊";
        public String advice = "建议先挂全科或内科，由首诊医生根据症状再决定是否转诊至专科；如症状突然加重或出现明显不适，应及时就近就医。";
    }

    public static class TriageRules {
        @JsonProperty("default")
        public TriageDefaultRule defaultRule = new TriageDefaultRule();
        public List<TriageRule> rules = new ArrayList<>();
    }

    public static class ReportTemplateBlock {
        public String findingDefault;
        public String impression;
    }

    public static class ReportTemplates {
        public ReportTemplateBlock bloodTemplate = new ReportTemplateBlock();
        public ReportTemplateBlock imagingTemplate = new ReportTemplateBlock();
        public ReportTemplateBlock generalTemplate = new ReportTemplateBlock();
        public String clinicalNoteTemplate;
        public String clinicalNoteFallback;
    }
}
