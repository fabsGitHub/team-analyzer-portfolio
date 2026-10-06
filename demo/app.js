const sampleGroups = {
  all: {
    participation: "86",
    responses: "12 of 14",
    pulse: "4.1",
    change: "+0.4",
    conversation: "+0.6",
    strongest: "Direction",
    copy: "People are feeling more supported by one another. What is helping that happen?",
    signals: [
      { name: "Clear on this week's priorities", score: "4.2", distribution: [0, 1, 2, 5, 4] },
      { name: "Comfortable asking for help", score: "4.0", distribution: [0, 2, 3, 4, 3] },
      { name: "Enough focus time", score: "3.5", distribution: [1, 2, 3, 4, 2] },
      { name: "Positive about our direction", score: "4.3", distribution: [0, 1, 2, 4, 5] }
    ]
  },
  product: {
    participation: "83",
    responses: "5 of 6",
    pulse: "4.3",
    change: "+0.6",
    conversation: "+0.8",
    strongest: "Support",
    copy: "The design group feels confident asking for input. How can that openness travel across the team?",
    signals: [
      { name: "Clear on this week's priorities", score: "4.4", distribution: [0, 0, 1, 2, 2] },
      { name: "Comfortable asking for help", score: "4.6", distribution: [0, 0, 1, 1, 3] },
      { name: "Enough focus time", score: "3.8", distribution: [0, 1, 1, 2, 1] },
      { name: "Positive about our direction", score: "4.4", distribution: [0, 0, 1, 1, 3] }
    ]
  },
  engineering: {
    participation: "88",
    responses: "7 of 8",
    pulse: "3.9",
    change: "+0.2",
    conversation: "+0.3",
    strongest: "Direction",
    copy: "Focus time is the most mixed signal. What is interrupting deep work, and what can the team change?",
    signals: [
      { name: "Clear on this week's priorities", score: "4.0", distribution: [0, 1, 1, 3, 2] },
      { name: "Comfortable asking for help", score: "3.7", distribution: [0, 2, 2, 2, 1] },
      { name: "Enough focus time", score: "3.3", distribution: [1, 1, 2, 2, 1] },
      { name: "Positive about our direction", score: "4.2", distribution: [0, 1, 1, 2, 3] }
    ]
  }
};

const surveyQuestions = [
  "I understand what matters most for our team right now.",
  "I can ask for help before a small blocker becomes a big one.",
  "I have enough focus time to do my best work."
];
const ratingLabels = [
  "Not at all",
  "A little",
  "Somewhat",
  "Mostly",
  "Very much"
];

const filter = document.getElementById("team-filter");
const signalList = document.getElementById("signal-list");
const dialog = document.getElementById("survey-dialog");
const questionArea = document.getElementById("survey-question-area");
const continueButton = document.getElementById("survey-continue");
const progress = document.getElementById("survey-progress");
const toast = document.getElementById("toast");
let activeGroup = "all";
let activeQuestion = 0;
let selectedRating = 0;
let toastTimer;

function text(id, value) {
  document.getElementById(id).textContent = value;
}

function makeSignalRow(signal) {
  const row = document.createElement("div");
  row.className = "signal-row";

  const label = document.createElement("span");
  label.className = "signal-name";
  label.textContent = signal.name;

  const distribution = document.createElement("div");
  distribution.className = "distribution";
  distribution.setAttribute("role", "img");
  distribution.setAttribute("aria-label", "Sample response distribution from 1 to 5");

  signal.distribution.forEach(function (count) {
    const segment = document.createElement("span");
    const total = signal.distribution.reduce(function (sum, item) {
      return sum + item;
    }, 0);
    segment.style.flexBasis = (count / total * 100) + "%";
    distribution.append(segment);
  });

  const score = document.createElement("span");
  score.className = "signal-score";
  score.textContent = signal.score;

  row.append(label, distribution, score);
  return row;
}

function renderGroup(groupName) {
  const group = sampleGroups[groupName] || sampleGroups.all;
  activeGroup = groupName;

  text("metric-participation", group.participation + "%");
  text("metric-responses", group.responses);
  text("metric-pulse", group.pulse + " / 5");
  text("metric-change", group.change);
  text("conversation-change", group.conversation);
  text("metric-strength", group.strongest);
  text("conversation-copy", group.copy);

  signalList.replaceChildren.apply(signalList, group.signals.map(makeSignalRow));

  document.querySelectorAll("[data-group-short]").forEach(function (item) {
    item.textContent = groupName === "all" ? "All team" : groupName === "product" ? "Product design" : "Engineering";
  });
}

filter.addEventListener("change", function (event) {
  renderGroup(event.target.value);
});

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(function () {
    toast.classList.remove("is-visible");
  }, 2600);
}

document.getElementById("export-results").addEventListener("click", function () {
  const group = sampleGroups[activeGroup];
  const rows = [["Sample question", "Average rating", "Distribution (1 to 5)"]];
  group.signals.forEach(function (signal) {
    rows.push([signal.name, signal.score, signal.distribution.join(" / ")]);
  });
  const csv = rows.map(function (row) {
    return row.map(function (value) {
      return '"' + String(value).replaceAll('"', '""') + '"';
    }).join(",");
  }).join("\n");
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "team-analyzer-sample-insights.csv";
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  showToast("Sample insight snapshot downloaded. It contains no personal responses.");
});

function renderSurveyQuestion() {
  selectedRating = 0;
  text("survey-step", "QUESTION " + (activeQuestion + 1) + " OF " + surveyQuestions.length);
  progress.style.width = ((activeQuestion + 1) / surveyQuestions.length * 100) + "%";
  text("survey-title", "A moment to reflect.");

  questionArea.replaceChildren();
  const question = document.createElement("p");
  question.className = "survey-question-label";
  question.textContent = surveyQuestions[activeQuestion];

  const list = document.createElement("div");
  list.className = "rating-list";
  list.setAttribute("role", "radiogroup");
  list.setAttribute("aria-label", "Choose your sample response");

  ratingLabels.forEach(function (rating, index) {
    const label = document.createElement("label");
    label.className = "rating-choice";

    const input = document.createElement("input");
    input.type = "radio";
    input.name = "sample-rating";
    input.value = String(index + 1);

    const choice = document.createElement("span");
    choice.textContent = rating;

    input.addEventListener("change", function () {
      selectedRating = Number(input.value);
      continueButton.disabled = false;
    });

    label.append(input, choice);
    list.append(label);
  });

  questionArea.append(question, list);
  continueButton.disabled = true;
  continueButton.innerHTML = activeQuestion === surveyQuestions.length - 1
    ? 'Finish sample <span aria-hidden="true">✓</span>'
    : 'Continue <span aria-hidden="true">→</span>';
}

function startSurvey() {
  activeQuestion = 0;
  document.getElementById("survey-shell").hidden = false;
  document.getElementById("survey-complete").hidden = true;
  renderSurveyQuestion();
  if (!dialog.open) dialog.showModal();
}

document.querySelectorAll("[data-open-survey]").forEach(function (button) {
  button.addEventListener("click", startSurvey);
});

continueButton.addEventListener("click", function () {
  if (!selectedRating) return;
  if (activeQuestion < surveyQuestions.length - 1) {
    activeQuestion += 1;
    renderSurveyQuestion();
    return;
  }

  document.getElementById("survey-shell").hidden = true;
  document.getElementById("survey-complete").hidden = false;
  progress.style.width = "100%";
});

document.getElementById("survey-restart").addEventListener("click", function () {
  activeQuestion = 0;
  document.getElementById("survey-complete").hidden = true;
  document.getElementById("survey-shell").hidden = false;
  renderSurveyQuestion();
});

renderGroup("all");
