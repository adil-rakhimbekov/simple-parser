package kz.parser.parser_baza_otvetov_ru2.dao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/****************************
 * @author adilrakhimbekov
 * @since 4/10/20
 ***************************/
@Slf4j
@Repository
public class QuestionDAO {
    @Autowired
    private DataSource dataSource;

    private SimpleJdbcInsert jdbcInsertQuestion;
    private SimpleJdbcInsert jdbcInsertAnswer;
    private JdbcTemplate template;

    @PostConstruct
    void postConstruct() {
        jdbcInsertQuestion = new SimpleJdbcInsert(dataSource)
                .withTableName("quiz_questions")
                .usingColumns("title", "body", "score")
                .usingGeneratedKeyColumns("id");
        jdbcInsertAnswer = new SimpleJdbcInsert(dataSource)
                .withTableName("quiz_answers")
                .usingColumns("question_id", "number", "body", "is_right")
                .usingGeneratedKeyColumns("id");
        template = new JdbcTemplate(dataSource);
    }

    public Long createQuestion(String title) {
        try {
            Long questionId = template.queryForObject("select id from quiz_questions where title = ? limit 1",
                    new String[]{title}, Long.class);
            if (questionId != null) {
                log.trace("Question {} allready existed", title);
                return questionId;
            }
        } catch (EmptyResultDataAccessException emptyResultExc) {
        }
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("title", title);
        parameters.put("body", title);
        parameters.put("score", 1);
        return (Long) jdbcInsertQuestion.executeAndReturnKey(parameters);
    }

    public void createAnswers(Long questionId, String rightAnswer, String[] wrongAnswers) {
        Integer number = template.queryForObject("select max(number) from quiz_answers where question_id = ?",
                new Long[]{questionId}, Integer.class);
        if (number == null) {
            number = 0;
        }
        if (number < 0 || number >= 4) {
            log.trace("QuestinId: {}, Answers number allready 4 or over", questionId);
            return;
        }
        number++;
        Long id = createAnswer(questionId, number, rightAnswer, true);

        for (String wrongAnswer : wrongAnswers) {
            if (wrongAnswer.trim().isEmpty()) {
                continue;
            }
            if (id != null) {
                number++;
            }
            if (number >= 5) {
                log.warn("QuestionId:{} Incoming answers number over 4. {}", questionId, Arrays.toString(wrongAnswers));
                return;
            }
            id = createAnswer(questionId, number, wrongAnswer.trim(), false);
        }
    }

    public Long createAnswer(Long questionId, Integer number, String answer, Boolean isRight) {
        try {
            Integer count = template.queryForObject("select count(id) from quiz_answers " +
                            "where body = ? and question_id = ?",
                    new Object[]{answer, questionId}, Integer.class);
            if (count != null && count > 0) {
                return null;
            }
        } catch (Exception exception) {
            log.trace("{} questionId:{} number:{} answer:{} isRight:{}",
                    exception.getMessage(),
                    questionId,
                    number,
                    answer,
                    isRight);
        }
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("question_id", questionId);
        parameters.put("number", number);
        parameters.put("body", answer);
        parameters.put("is_right", isRight);
        return (Long) jdbcInsertAnswer.executeAndReturnKey(parameters);
    }

}
