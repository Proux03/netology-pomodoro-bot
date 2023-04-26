package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        PomodoroBot pomodoroBot = new PomodoroBot();
        telegramBotsApi.registerBot(pomodoroBot);
        new Thread(() -> {
            try {
                pomodoroBot.checkTimer();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    static class PomodoroBot extends TelegramLongPollingBot {

        private static final ConcurrentHashMap<Timer, Long> timers = new ConcurrentHashMap<>();

        enum TimerType {
            WORK,
            BREAK
        }

        record Timer(Instant timer, TimerType timerType){};

        @Override
        public void onUpdateReceived(Update update) {
            if(update.hasMessage() && update.getMessage().hasText()) {
                Long chatId = update.getMessage().getChatId();
                if(update.getMessage().getText().equals("/start")) {
                    sendMsg(chatId.toString(), "Pomodoro - сделай своё время более эффективным. " +
                            "Задай мне время работы и отдыха в минутах, через пробел. Например, '1 1'.");
                } else {
                    String[] args = update.getMessage().getText().split(" ");
                    if (args.length >= 1) {
                        Instant workTime = Instant.now().plus(Long.parseLong(args[0]), ChronoUnit.MINUTES);
                        timers.put(new Timer(workTime, TimerType.WORK), chatId);


                        if (args.length >= 2) {
                            Instant breakTime = workTime.plus(Long.parseLong(args[1]), ChronoUnit.MINUTES);
                            timers.put(new Timer(breakTime, TimerType.BREAK), chatId);
                        }
                    }
                }
            }
        }

        public void checkTimer() throws InterruptedException {
            while(true) {
                System.out.println("Количество таймеров пользователей " + timers.size());
                timers.forEach((timer, userId) -> {
                    if(Instant.now().isAfter(timer.timer)) {
                        timers.remove(timer);
                        switch (timer.timerType) {
                            case WORK -> sendMsg(userId.toString(), "Пора отдыхать");
                            case BREAK -> sendMsg(userId.toString(), "Таймер завершил свою работу");
                        }
                    }
                });
                Thread.sleep(1000);
            }
        }

        public String getBotToken() {
            return "5660617872:AAF2GEwCoqfXYf5IG55KSeMA5u0R0aSWaA0";
        }

        @Override
        public String getBotUsername() {
            return "Pomodoro бот";
        }

        private void sendMsg(String chatId, String text) {
            SendMessage msg = new SendMessage(chatId, text);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class EchoBot extends TelegramLongPollingBot {

        private static int userCount = 0;

        @Override
        public void onUpdateReceived(Update update) {
            if(update.hasMessage() && update.getMessage().hasText()) {
                String chatId = update.getMessage().getChatId().toString();
                if(update.getMessage().getText().equals("/start")) {
                    userCount++;
                    sendMsg(chatId, "Привет, я попугай бот! " +
                            "Буду повторять всё за тобой)");
                } else {
                    sendMsg(chatId, update.getMessage().getText());
                }
            }
            System.out.println("Количество пользователей " + userCount);
        }

        private void sendMsg(String chatId, String text) {
            SendMessage msg = new SendMessage(chatId, text);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

        public String getBotToken() {
            return "5660617872:AAF2GEwCoqfXYf5IG55KSeMA5u0R0aSWaA0";
        }

        @Override
        public String getBotUsername() {
            return "Попугай бот";
        }
    }
}