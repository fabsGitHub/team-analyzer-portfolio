package com.teamanalyzer.teamanalyzer.service;

import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.teamanalyzer.teamanalyzer.domain.Survey;
import com.teamanalyzer.teamanalyzer.domain.SurveyAnswer;
import com.teamanalyzer.teamanalyzer.domain.SurveyAnalytics;
import com.teamanalyzer.teamanalyzer.domain.SurveyQuestion;
import com.teamanalyzer.teamanalyzer.domain.SurveyResponse;
import com.teamanalyzer.teamanalyzer.domain.SurveyToken;
import com.teamanalyzer.teamanalyzer.domain.Team;
import com.teamanalyzer.teamanalyzer.port.DigestService;
import com.teamanalyzer.teamanalyzer.repo.SurveyQuestionRepository;
import com.teamanalyzer.teamanalyzer.repo.SurveyRepository;
import com.teamanalyzer.teamanalyzer.repo.SurveyResponseRepository;
import com.teamanalyzer.teamanalyzer.repo.TeamMemberRepository;
import com.teamanalyzer.teamanalyzer.web.dto.SurveyDto;
import com.teamanalyzer.teamanalyzer.web.dto.SurveyResultsDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SurveyService {

    public static final int QUESTION_COUNT = 5;

    private final SurveyRepository surveyRepo;
    private final SurveyQuestionRepository questionRepo;
    private final SurveyResponseRepository responseRepo;
    private final TeamMemberRepository tmRepo;
    private final TokenService tokenService;
    private final DigestService digest;

    @Transactional
    public Survey createSurvey(UUID leaderId, UUID teamId, String title, List<String> qTexts) {
        if (!tmRepo.existsByTeam_IdAndUser_IdAndLeaderTrue(teamId, leaderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "error.surveys.onlyLeader");
        }
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.surveys.titleRequired");
        }
        if (qTexts == null || qTexts.size() != QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.surveys.exactQuestionCount");
        }

        var s = Survey.create(Team.ref(teamId), leaderId, title);
        surveyRepo.save(s);

        List<SurveyQuestion> questions = IntStream.range(0, QUESTION_COUNT)
                .mapToObj(i -> {
                    var q = new SurveyQuestion();
                    q.setSurvey(s);
                    q.setIdx((short) (i + 1));
                    q.setText(qTexts.get(i));
                    return q;
                }).toList();

        questionRepo.saveAll(questions);
        return s;
    }

    @Transactional(readOnly = true)
    public SurveyDto getSurvey(UUID id) {
        Survey s = surveyRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<SurveyQuestion> qs = questionRepo.findBySurveyIdOrderByIdx(id);
        return SurveyDto.from(s, qs);
    }

    /** Plain → SHA-256 → hex → delegiert. */
    @Transactional
    public void submitAnonymousByPlainToken(UUID surveyId, String plainToken, short[] answers) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing token");
        }
        String tokenHashHex = HexFormat.of().formatHex(digest.sha256(plainToken));
        submitAnonymous(surveyId, tokenHashHex, answers);
    }

    /**
     * Token wird in derselben Tx gelockt & erst nach erfolgreichem Persist
     * verbraucht.
     */
    @Transactional
    public void submitAnonymous(UUID surveyId, String tokenHashHex, short[] answers) {
        if (answers == null || answers.length != QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.surveys.answerCount");
        }
        short[] safe = answers.clone();
        for (int i = 0; i < QUESTION_COUNT; i++) {
            if (safe[i] < 1 || safe[i] > 5) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.surveys.answerRange");
            }
        }

        var tok = tokenService.acquireForSubmission(surveyId, tokenHashHex);
        var qs = questionRepo.findBySurveyIdOrderByIdx(surveyId);
        if (qs.size() != QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "error.surveys.missingQuestions");
        }

        var r = SurveyResponse.create(Survey.ref(surveyId), tok);
        for (int i = 0; i < QUESTION_COUNT; i++) {
            var a = new SurveyAnswer();
            a.setQuestion(qs.get(i));
            a.setValue(safe[i]); // Likert-Wert
            a.setAnswerOrder(i); // 0..4
            r.addAnswer(a);
        }
        responseRepo.save(r);
        tokenService.consume(tok);
    }

    // Kompatibilitäts-Overloads
    @Transactional
    public void submitAnonymous(UUID surveyId, SurveyToken tok, short[] answers) {
        submitAnonymous(surveyId, tok.getTokenHash(), answers);
    }

    @Transactional
    public void submitAnonymous(UUID surveyId, byte[] tokenHash, short[] answers) {
        submitAnonymous(surveyId, HexFormat.of().formatHex(tokenHash), answers);
    }

    @Transactional(readOnly = true)
    public SurveyResultsDto getResults(UUID surveyId) {
        List<SurveyResponse> all = responseRepo.findBySurveyId(surveyId);
        double[] avg = SurveyAnalytics.averages(all);
        int n = all.size();

        var items = all.stream()
                .map(com.teamanalyzer.teamanalyzer.web.dto.SingleSurveyResultDto::from)
                .toList();

        return com.teamanalyzer.teamanalyzer.web.dto.SurveyResultsDto.of(
                avg[0], avg[1], avg[2], avg[3], avg[4], n, items);
    }
}
