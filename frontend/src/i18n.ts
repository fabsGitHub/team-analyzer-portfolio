// frontend/src/i18n.ts
import { createI18n } from 'vue-i18n'
const de = {
  app: 'Team Radar',
  auth: {
    title: 'Anmelden',
    signin: 'Einloggen',
    signup: 'Registrieren',
    name: 'Name',
    go: 'Weiter',
    password: 'Passwort',
    email: 'E-Mail',
    exists:
      'Diese E-Mail ist bereits registriert. Bitte anmelden oder Passwort zurücksetzen.',
    reset: 'Passwort vergessen?',
    reset_sent:
      'Falls registriert, wurde eine E-Mail zum Zurücksetzen gesendet.',
    enter_email: 'Bitte E-Mail-Adresse eingeben.',
    signingin: 'Melde an…',
    verified_ok: 'E-Mail bestätigt – bitte anmelden.',
    verified_failed: 'Verifizierungslink ist ungültig oder abgelaufen.',
    check_inbox: 'Registriert. Bitte E-Mail bestätigen.',
    check_mail: 'Registriert. Bitte E-Mail bestätigen.', // Alias für Store
    resetPassword: {
      title: 'Passwort zurücksetzen',
      new: 'Neues Passwort',
      repeat: 'Passwort wiederholen',
      show: 'Anzeigen',
      hide: 'Verbergen',
      minlen: 'Mindestens {n} Zeichen',
      mismatch: 'Passwörter stimmen nicht überein.',
      save: 'Speichern',
      saving: 'Speichern…',
      tokenMissing: 'Kein Token in der URL gefunden.',
      ok: 'Passwort aktualisiert. Du kannst dich jetzt anmelden.',
      badToken: 'Token ungültig oder abgelaufen.',
      genericErr: 'Fehler beim Zurücksetzen',
      cantSubmit: 'Speichern nicht möglich:',
      fix: 'Bitte Eingaben prüfen.',
      err: {
        minlen: 'Passwort muss mindestens {n} Zeichen lang sein.',
        mismatch: 'Passwörter stimmen nicht überein.',
        busy: 'Es wird bereits gespeichert. Bitte warten.',
      },
    },
  },
  nav: {
    evaluate: 'Bewerten',
    analysis: 'Analyse',
    tutorial: 'Tutorial',
    admin: 'Admin',
    createSurvey: 'Umfrage anlegen',
    myTokens: 'Meine Tokens',
    mySurveys: 'Meine Umfragen',
    language_word: 'Sprache',
    language: {
      de: 'Deutsch',
      en: 'Englisch',
    },
  },
  form: {
    add: 'Neue Bewertung',
    edit: 'Bewertung bearbeiten',
    name: 'Name der Person',
    team: 'Team',
    save: 'Speichern',
    cancel: 'Abbrechen',
    mySurveyTokens: 'Meine Umfrage-Tokens',
    confirm: 'Bestätigen',
    close: 'Schließen',
    refresh: 'Aktualisieren',
  },
  admin: {
    teams: {
      title: 'Teamverwaltung',
      newTitle: 'Neues Team erstellen',
      namePlaceholder: 'Team-Name',
      leaderIdPlaceholder: 'Leader-UserId (UUID)',
      createBtn: 'Anlegen',
      membersCount: 'Mitglieder: {n}',
      leadersCount: 'Leader: {n}',
      deleteTeam: 'Team löschen',
      userId: 'User ID',
      leader: 'Leader',
      actions: 'Aktionen',
      makeLeader: 'Leader machen',
      removeLeader: 'Leader entziehen',
      removeMember: 'Entfernen',
      noneMembers: 'Noch keine Mitglieder',
      addMember: {
        userIdPlaceholder: 'UserId (UUID)',
        isLeader: 'Leader?',
        addBtn: 'Hinzufügen',
      },
      confirmDelete:
        'Soll das Team wirklich gelöscht werden? Alle Mitglieder werden entfernt!',
      noneTeams: 'Noch keine Teams angelegt. Lege oben ein Team an.',
      removeMemberTitle: 'Mitglied entfernen',
      confirmRemoveMember:
        'Soll das Mitglied {userId} aus „{team}“ entfernt werden?',
      confirmDeleteTeamWithName:
        'Soll das Team „{team}“ wirklich gelöscht werden? Alle Mitglieder werden entfernt!',
    },
  },
  tokens: {
    title: 'Meine offenen Tokens',
    loading: 'Lade…',
    none: 'Keine offenen Tokens.',
    issuedAt: 'ausgestellt:',
    openSurvey: 'Zur Umfrage',
    openError: 'Fehler beim Öffnen der Umfrage',
  },
  surveys: {
    create: {
      title: 'Survey erstellen',
      meta: 'Stammdaten',
      fields: { title: 'Titel', team: 'Team auswählen', submit: 'Anlegen' },
      questions: { title: 'Fragen (5)', placeholder: 'Frage {i}' },
      issuedTeamTokensToast: '{n} neue Tokens für Teammitglieder erstellt.',
    },
    mine: {
      title: 'Meine Surveys',
      entries: '{n} Einträge',
      actions: {
        details: 'Details',
        results: 'Ergebnisse',
        jsonExport: 'JSON-Export',
      },
      empty: 'Noch keine Surveys vorhanden.',
      teamFallback: '—',
    },
  },
  results: {
    title: 'Survey-Ergebnisse',
    back: '← Zur Übersicht',
    json: 'JSON',
    csv: 'CSV',
    reload: 'Neu laden',
    loading: 'Ergebnisse werden geladen…',
    errorTitle: 'Fehler:',
    reauthHint: 'Bitte melde dich erneut an.',
    empty: 'Noch keine Antworten vorhanden.',
    value: 'Wert',
    total: 'Summe',
    count: 'Anzahl',
    participants: 'Teilnahmen: {n}',
    starsTooltip: '{n} Sterne: {count}',
    onlyLeader:
      'Nur der Ersteller (Leader) einer Umfrage kann die Ergebnisse sehen.',
    location: 'Die Ergebnisse findest du unter „Meine Umfragen“.',
    download:
      'Dort kannst du auch einen Download-Link (/api/surveys/[id]/results) erstellen, um die Ergebnisse als JSON herunterzuladen.',
    validity:
      'Der Link ist nur gültig, wenn er vorher von einem Leader erzeugt wurde.',
  },
  survey: {
    fill: {
      token: {
        label: 'Teilnahmetoken',
        placeholder: 'Token aus dem Einladungslink',
        help: 'Der Token ist im Einladungslink enthalten.',
      },
      answered: '{n}/5 beantwortet',
      submit: 'Absenden',
      loading: 'Survey wird geladen…',
    },
  },
  verify: {
    title: 'E-Mail bestätigen',
    status: {
      idle: 'Bestätige…',
      missing: 'Kein Token in der URL gefunden.',
      error: 'Bestätigung fehlgeschlagen. Bitte Link erneut öffnen.',
      ok: 'Bestätigt — weiterleiten…',
    },
  },
  categories: {
    appreciation: 'Wertschätzung',
    equality: 'Gleichwertigkeit',
    workload: 'Arbeitsbelastung',
    collegiality: 'Umgang',
    transparency: 'Transparenz',
  },
  categoryLabels: {
    appreciation: '@:categories.appreciation',
    equality: '@:categories.equality',
    workload: '@:categories.workload',
    collegiality: '@:categories.collegiality',
    transparency: '@:categories.transparency',
  },
  table: {
    actions: 'Aktionen',
    edit: 'Bearbeiten',
    del: 'Löschen',
    empty: 'Noch keine Bewertungen.',
    name: 'Name',
  },
  analysis: {
    pick: 'Teams auswählen (max. 3)',
    details: 'Details für 5 Kategorien (1–5) ein und speichere.',
    compare: 'Team-Vergleich',
    avg: 'Durchschnitt',
    selected: 'ausgewählt',
    entries: 'Einträge',
    tip: 'Tipp: Auf eine Team-Badge klicken, um Details zu öffnen.',
    desc: 'Radar-Diagramm mit Durchschnittswerten für 5 Kategorien.',
  },
  toast: {
    saved: 'Gespeichert',
    deleted: 'Gelöscht',
    updated: 'Aktualisiert',
    signedin: 'Angemeldet',
    signedout: 'Abgemeldet',
  },
  footer: {
    info: 'Info & Rechtliches',
    imprint: 'Impressum',
    privacy: 'Datenschutz',
    help: 'Hilfe',
    shortcuts: 'Tastenkürzel',
    about: 'Über Team Radar',
  },
  user: {
    account: 'Konto',
    logout: 'Abmelden',
    email: 'E-Mail',
  },
  tutorial: {
    quickstart: 'Schnellstart',
    step1:
      'Gib eine Person, ein Team und Bewertungen für 5 Kategorien (1–5) ein und speichere.',
    step2:
      'Wähle bis zu 3 Teams aus, vergleiche sie im Radar-Diagramm und klicke auf ein Badge für Details.',
    tip: 'Tipp',
    step3: 'Mit Strg/Cmd oder Shift kannst du mehrere Teams auswählen.',
    guide: {
      adminTitle: 'Admin',
      admin:
        'Nur sichtbar für Nutzer mit der Rolle Admin. Auf der Admin-Seite kannst du Teams anlegen/verwalten und Teamleiter zuweisen.',
      leaderTitle: 'Teamleiter',
      leader:
        'Teamleiter können Umfragen für ein Team erstellen. Beim Erstellen einer Umfrage wird automatisch ein Token pro Teammitglied generiert (z. B. 5 Mitglieder → 5 Tokens).',
      memberTitle: 'Teammitglieder',
      member:
        'Öffne „Meine Tokens“, wähle einen offenen Token und starte die Umfrage — oder nutze den Einladungslink per E-Mail. Jeder Token ist nur einmal nutzbar und verfällt nach der Abgabe.',
      anonTitle: 'Teilnahme ohne Anmeldung',
      anon: 'Über die direkte Umfrage-URL mit Token können Nutzer ohne Anmeldung teilnehmen. Alle anderen Seiten erfordern weiterhin eine Anmeldung.',
      resultsTitle: 'Umfrageergebnisse',
      results:
        'Nur der Ersteller (Leader) einer Umfrage kann die Ergebnisse sehen. Die Ergebnisse findest du unter „Meine Umfragen“. Dort kannst du auch einen Download-Link (/api/surveys/[id]/results) erstellen, um die Ergebnisse als JSON herunterzuladen. Der Link ist nur gültig, wenn er vorher von einem Leader erzeugt wurde.',
    },
  },
  misc: {
    maxTeams: 'Max 3 Teams.',
    ctrl: 'Strg/Cmd',
    shift: 'Shift',
    noData: 'Keine Daten/Teams ausgewählt.',
  },
}
const en = {
  app: 'Team Radar',
  auth: {
    title: 'Sign in',
    signin: 'Sign in',
    signup: 'Sign up',
    name: 'Name',
    go: 'Continue',
    exists:
      'This email is already registered. Please sign in or reset your password.',
    reset: 'Forgot password?',
    reset_sent: 'If registered, a reset email has been sent.',
    enter_email: 'Please enter your email address.',
    signingin: 'Signing in…',
    verified_ok: 'Email verified — please sign in.',
    verified_failed: 'Verification link is invalid or expired.',
    check_inbox: 'Registered. Please confirm your email.',
    check_mail: 'Registered. Please confirm your email.', // alias for store
    resetPassword: {
      title: 'Reset password',
      new: 'New password',
      repeat: 'Repeat password',
      show: 'Show',
      hide: 'Hide',
      minlen: 'At least {n} characters',
      mismatch: 'Passwords do not match.',
      save: 'Save',
      saving: 'Saving…',
      tokenMissing: 'No token found in URL.',
      ok: 'Password updated. You can sign in now.',
      badToken: 'Token invalid or expired.',
      genericErr: 'Failed to reset password',
      cantSubmit: 'Cannot save:',
      fix: 'Please check your input.',
      err: {
        minlen: 'Password must be at least {n} characters.',
        mismatch: 'Passwords do not match.',
        busy: 'A save is already in progress. Please wait.',
      },
    },
  },
  nav: {
    evaluate: 'Evaluate',
    analysis: 'Analysis',
    tutorial: 'Tutorial',
    admin: 'Admin',
    createSurvey: 'Create survey',
    myTokens: 'My tokens',
    mySurveys: 'My surveys',
    language_word: 'Language',
    language: {
      de: 'German',
      en: 'English',
    },
  },
  form: {
    add: 'New Evaluation',
    edit: 'Edit Evaluation',
    name: 'Person name',
    team: 'Team',
    save: 'Save',
    cancel: 'Cancel',
    mySurveyTokens: 'My survey tokens',
    confirm: 'Confirm',
    close: 'Close',
    refresh: 'Refresh',
  },
  admin: {
    teams: {
      title: 'Team management',
      newTitle: 'Create new team',
      namePlaceholder: 'Team name',
      leaderIdPlaceholder: 'Leader userId (UUID)',
      createBtn: 'Create',
      membersCount: 'Members: {n}',
      leadersCount: 'Leaders: {n}',
      deleteTeam: 'Delete team',
      userId: 'User ID',
      leader: 'Leader',
      actions: 'Actions',
      makeLeader: 'Make leader',
      removeLeader: 'Remove leader',
      removeMember: 'Remove',
      noneMembers: 'No members yet',
      addMember: {
        userIdPlaceholder: 'UserId (UUID)',
        isLeader: 'Leader?',
        addBtn: 'Add',
      },
      confirmDelete: 'Really delete this team? All members will be removed!',
      noneTeams: 'No teams yet. Create one above.',
      removeMemberTitle: 'Remove member',
      confirmRemoveMember:
        'Do you really want to remove member {userId} from “{team}”?',
      confirmDeleteTeamWithName:
        'Really delete team “{team}”? All members will be removed!',
    },
  },
  tokens: {
    title: 'My open tokens',
    loading: 'Loading…',
    none: 'No open tokens.',
    issuedAt: 'issued:',
    openSurvey: 'Open survey',
    openError: 'Failed to open survey',
  },
  surveys: {
    create: {
      title: 'Create survey',
      meta: 'Master data',
      fields: { title: 'Title', team: 'Select team', submit: 'Create' },
      questions: { title: 'Questions (5)', placeholder: 'Question {i}' },
      issuedTeamTokensToast: 'Created {n} new tokens for team members.',
    },
    mine: {
      title: 'My surveys',
      entries: '{n} entries',
      actions: {
        details: 'Details',
        results: 'Results',
        jsonExport: 'JSON export',
      },
      empty: 'No surveys yet.',
      teamFallback: '—',
    },
  },
  results: {
    title: 'Survey results',
    back: '← Back to overview',
    json: 'JSON',
    csv: 'CSV',
    reload: 'Reload',
    loading: 'Loading results…',
    errorTitle: 'Error:',
    reauthHint: 'Please sign in again.',
    empty: 'No responses yet.',
    value: 'Value',
    total: 'Total',
    count: 'Count',
    participants: 'Participants: {n}',
    starsTooltip: '{n} stars: {count}',
    onlyLeader: 'Only the creator (Leader) of a survey can view its results.',
    location: "You can find the results under 'My Surveys'.",
    download:
      'There you can also generate a download link (/api/surveys/[id]/results) to download the results as JSON.',
    validity:
      'The link is only valid if it was generated by a Leader beforehand.',
  },
  survey: {
    fill: {
      token: {
        label: 'Participation token',
        placeholder: 'Token from the invite link',
        help: 'The token is included in the invite link.',
      },
      answered: '{n}/5 answered',
      submit: 'Submit',
      loading: 'Loading survey…',
    },
  },
  verify: {
    title: 'Verify email',
    status: {
      idle: 'Verifying…',
      missing: 'No token found in URL.',
      error: 'Verification failed. Please open the link again.',
      ok: 'Verified — redirecting…',
    },
  },
  categories: {
    appreciation: 'Appreciation',
    equality: 'Equality',
    workload: 'Workload',
    collegiality: 'Collegiality',
    transparency: 'Transparency',
  },
  categoryLabels: {
    appreciation: '@:categories.appreciation',
    equality: '@:categories.equality',
    workload: '@:categories.workload',
    collegiality: '@:categories.collegiality',
    transparency: '@:categories.transparency',
  },
  table: {
    actions: 'Actions',
    edit: 'Edit',
    del: 'Delete',
    empty: 'No evaluations yet.',
    name: 'Name',
    password: 'Password',
    email: 'E-Mail',
  },
  analysis: {
    pick: 'Select teams (max. 3)',
    details: 'Details for 5 categories (1–5), then save.',
    compare: 'Team comparison',
    avg: 'Average',
    selected: 'selected',
    entries: 'entries',
    desc: 'Radar chart with average values for 5 categories.',
    tip: 'Tip: Click on a team badge to view details.',
  },
  toast: {
    saved: 'Saved',
    deleted: 'Deleted',
    updated: 'Updated',
    signedin: 'Signed in',
    signedout: 'Signed out',
  },
  footer: {
    info: 'Info & Legal',
    imprint: 'Imprint',
    privacy: 'Privacy',
    help: 'Help',
    shortcuts: 'Shortcuts',
    about: 'About Team Radar',
  },
  user: {
    account: 'Account',
    logout: 'Logout',
    email: 'E-Mail',
  },
  tutorial: {
    quickstart: 'Quickstart',
    step1:
      'Enter a person, a team, and ratings for 5 categories (1–5), then save.',
    step2:
      'Select up to 3 teams, compare them in the radar chart, and click a badge for details.',
    tip: 'Tip',
    step3: 'Use Ctrl/Cmd or Shift to select multiple teams.',
    guide: {
      adminTitle: 'Admin',
      admin:
        'Visible only to users with the Admin role. On the admin page you can create/manage teams and assign team leaders.',
      leaderTitle: 'Team lead',
      leader:
        'Team leads can create surveys for a team. When a survey is created, one token per team member is generated automatically (e.g. 5 members → 5 tokens).',
      memberTitle: 'Team members',
      member:
        'Open “My tokens”, pick an open token, and start the survey — or use the invite link via email. Each token is single-use and expires after submission.',
      anonTitle: 'Participation without login',
      anon: 'Using the direct survey URL containing the token, users can participate without signing in. All other pages still require a login.',
      resultsTitle: 'Survey results',
      results:
        "Only the creator (Leader) of a survey can view its results. You can find the results under 'My Surveys'. There you can also generate a download link (/api/surveys/[id]/results) to get the results as JSON. The link is only valid if it was generated by a Leader beforehand.",
    },
  },
  misc: {
    maxTeams: 'Max 3 Teams.',
    ctrl: 'Ctrl/Cmd',
    shift: 'Shift',
    noData: 'No data/teams selected.',
  },
}

function initialLocale(): 'de' | 'en' {
  try {
    const ss =
      typeof window !== 'undefined'
        ? sessionStorage.getItem('app.lang') || localStorage.getItem('app.lang')
        : null
    if (ss === 'de' || ss === 'en') return ss
  } catch {}
  const nav = typeof navigator !== 'undefined' ? navigator.language : 'de'
  return nav?.startsWith('de') ? 'de' : 'en'
}

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale(),
  fallbackLocale: 'en',
  messages: { de, en },
})
