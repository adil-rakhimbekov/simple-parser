package kz.parser.parser_baza_otvetov_ru2.runners;

import kz.parser.parser_baza_otvetov_ru2.dao.QuestionDAO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/****************************
 * @author adilrakhimbekov
 * @since 4/10/20
 ***************************/
@Component
@Slf4j
@RequiredArgsConstructor
public class MainRunner implements CommandLineRunner {

    private final QuestionDAO questionDAO;

    @Value("${target.url}")
    String mainUrl;

    @Override
    public void run(String... args) {
        StopWatch timer = new StopWatch("Main");
        String pageOffset = "";
        Integer offset = 0;
        String url = mainUrl;
        timer.start(url);
        while (parsePage(url) == 0) {
            try {
                timer.stop();
                log.debug("URL:{} Offset: {} Task elapsed:{} sec, Job elapse:{} sec",
                        url, pageOffset, timer.getLastTaskTimeMillis()/1000.0, timer.getTotalTimeMillis()/1000.0);

                offset += 10;
                pageOffset = offset.toString();
                url = mainUrl + pageOffset;
                timer.start(url);
            }catch (Exception e){
                log.error(e.getMessage());
                break;
            }
        }
        timer.stop();
        log.debug(timer.prettyPrint());
    }

    private int parsePage(String url) {
        try {
            log.trace("URL:{}",url);
            Document doc = Jsoup.connect(url).timeout(60000).get();
            if(doc==null){
                log.error("Document is null. Url:{}",url);
                return -1;
            }
            new Thread(()->{
                UUID threadId = UUID.randomUUID();
                for(int row=0; row<10;row++) {
                    try {
                        // Question
                        String question = doc.getElementsByClass("tooltip")
                                .get(row)
                                .getElementsByTag("a")
                                .text();
                        log.trace("[{}] Question:{}", threadId, question);
                        if (question == null && question.isEmpty()) {
                            log.warn("[{}] URL:{} Rows less than:{}", url, row+1);
                        }
                        Long questionId = questionDAO.createQuestion(question);

                        log.trace("[{}] QuestionId:{}", threadId, questionId);
                        // Wrong answers
                        String[] wrongAnswers = doc.getElementsByClass("tooltip")
                                .get(row)
                                .getElementsByClass("q-list__quiz-answers")
                                .text()
                                .replace("Ответы для викторин: ", "")
                                .split(",");
                        log.trace("[{}] QuestionId:{} Wrong answers:{}", threadId, questionId, Arrays.toString(wrongAnswers));
                        // Right answer
                        String rightAnswer = doc.getElementsByClass("tooltip")
                                .get(row)
                                .getElementsByTag("td")
                                .get(2)
                                .text().trim();
                        log.trace("[{}] QuestionId:{} RightAnswer:{}", threadId, questionId, rightAnswer);
                        questionDAO.createAnswers(questionId, rightAnswer, wrongAnswers);
                    }catch (IndexOutOfBoundsException oversize){
                        log.warn("[{}] URL:{} Rows less than:{}", threadId, url, row+1);
                        break;
                    }
                }
            }).start();
        } catch (HttpStatusException httpStatusException) {
            log.warn(httpStatusException.getMessage());
            return -1;
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return 0;
    }
}
