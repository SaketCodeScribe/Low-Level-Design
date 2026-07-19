package Problems.SocialAndContentPlatforms.CricInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CricInfo {

    enum PlayerRole {
        BATSMAN,
        BOWLER,
        ALL_ROUNDER;
    }

    enum WicketType {
        LBW,
        RUN_OUT,
        CATCH_OUT,
        STUMPED,
        BOLD;
    }

    enum ExtraType {
        WIDE,
        NO_BALL,
        BYE,
        LEG_BYE
    }

    static interface Observer {
        void updateStateChange(Match match, Ball ball);
    }

    static interface Observable {
        void notifyObservers(Ball ball);
    }

    static interface MatchFormat {
        int getOvers();

        int noOfInnings();

        String getScore(Match match);
    }

    static interface MatchState {
        default String coinToss(Map<String, String> call, String coinFace) {
            throw new IllegalStateException("coinToss not allowed in " + getClass().getSimpleName());
        }

        default Innings startInnings(Team batting, Team bowling) {
            throw new IllegalStateException("startInnings not allowed in " + getClass().getSimpleName());
        }

        default Innings nextInnings(Match match) {
            throw new IllegalStateException("nextInnings not allowed in " + getClass().getSimpleName());
        }

        default Team winner(Match match) {
            throw new IllegalStateException("winner not allowed in " + getClass().getSimpleName());
        }

        default void updateBall(Ball ball, Innings innings) {
            throw new IllegalStateException("updateBall not allowed in " + getClass().getSimpleName());
        }

        default boolean hasMatchEnded(Match match) {
            throw new IllegalStateException("hasMatchEnded not allowed in " + getClass().getSimpleName());
        }

        default void setAbandoningReason(Match match, String reason) {
            throw new IllegalStateException("setAbandoningReason not allowed in " + getClass().getSimpleName());
        }
    }

    static class PlayerStats {
        int runsScored;
        int ballsPlayed;
        int wicketsTaken;
        int runsConceded;
        int ballsBalled;

        public int getRunsScored() {
            return runsScored;
        }

        public void setRunsScored(int runsScored) {
            this.runsScored = runsScored;
        }

        public int getBallsPlayed() {
            return ballsPlayed;
        }

        public void setBallsPlayed(int ballsPlayed) {
            this.ballsPlayed = ballsPlayed;
        }

        public int getWicketsTaken() {
            return wicketsTaken;
        }

        public void setWicketsTaken(int wicketsTaken) {
            this.wicketsTaken = wicketsTaken;
        }

        public int getRunsConceded() {
            return runsConceded;
        }

        public void setRunsConceded(int runsConceded) {
            this.runsConceded = runsConceded;
        }

        public int getBallsBalled() {
            return ballsBalled;
        }

        public void setBallsBalled(int ballsBowled) {
            this.ballsBalled = ballsBowled;
        }
    }

    static class Player {
        String id;
        String name;
        PlayerRole role;
        PlayerStats playerStats;

        public Player(String id, String name, PlayerRole role) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.playerStats = new PlayerStats();
        }

        public PlayerStats getPlayerStats() {
            return playerStats;
        }

        @Override
        public String toString() {
            return "Player{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    static class TeamStats {
        int matchesWon;
        int matchesLost;
        int matchesDrawn;
        int matchesPlayed;

        public int getMatchesWon() {
            return matchesWon;
        }

        public void setMatchesWon(int matchesWon) {
            this.matchesWon = matchesWon;
        }

        public int getMatchesLost() {
            return matchesLost;
        }

        public void setMatchesLost(int matchesLost) {
            this.matchesLost = matchesLost;
        }

        public int getMatchesDrawn() {
            return matchesDrawn;
        }

        public void setMatchesDrawn(int matchesDrawn) {
            this.matchesDrawn = matchesDrawn;
        }

        public int getMatchesPlayed() {
            return matchesPlayed;
        }

        public void setMatchesPlayed(int matchesPlayed) {
            this.matchesPlayed = matchesPlayed;
        }
    }

    static class Team {
        String name;
        Set<Player> players;
        TeamStats teamStats;

        public Team(String name) {
            this.name = name;
            this.players = new HashSet<>();
            teamStats = new TeamStats();
        }

        public void addPlayer(Player player) {
            players.add(player);
        }

        public void removePlayer(Player player) {
            players.remove(player);
        }

        public TeamStats getTeamStats() {
            return teamStats;
        }
    }

    static class Wicket {
        WicketType wicketType;
        Player playerOut;
        Player balledBy;
        Player caughtBy;
        Player runOutBy;
        Player stumpedBy;

        private Wicket(WicketType wicketType, Player playerOut, Player balledBy, Player caughtBy, Player runOutBy, Player stumpedBy) {
            this.wicketType = wicketType;
            this.playerOut = playerOut;
            this.balledBy = balledBy;
            this.caughtBy = caughtBy;
            this.runOutBy = runOutBy;
            this.stumpedBy = stumpedBy;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return "Wicket{" +
                    "wicketType=" + wicketType +
                    ", playerOut=" + playerOut +
                    ", balledBy=" + balledBy +
                    ", caughtBy=" + caughtBy +
                    ", runOutBy=" + runOutBy +
                    ", stumpedBy=" + stumpedBy +
                    '}';
        }

        static class Builder {
            WicketType wicketType;
            Player playerOut;
            Player balledBy;
            Player caughtBy;
            Player runOutBy;
            Player stumpedBy;

            public Builder withWicketType(WicketType wicketType) {
                this.wicketType = wicketType;
                return this;
            }

            public Builder withPlayerOut(Player player) {
                this.playerOut = player;
                return this;
            }

            public Builder withBalledBy(Player player) {
                this.balledBy = player;
                return this;
            }

            public Builder withCaughtBy(Player player) {
                this.caughtBy = player;
                return this;
            }

            public Builder withRunOutBy(Player player) {
                this.runOutBy = player;
                return this;
            }

            public Builder withStumpedBy(Player player) {
                this.stumpedBy = player;
                return this;
            }

            public Wicket build() {
                return new Wicket(this.wicketType, this.playerOut, this.balledBy, this.caughtBy, this.runOutBy, this.stumpedBy);
            }
        }
    }

    static class Ball {
        Player balledBy;
        Player playedBy;
        Player nonStriker;
        Wicket wicket;
        boolean isWicket;
        int runsScored;
        ExtraType extra;

        private Ball(Player balledBy, Player playedBy, Wicket wicket,
                     int runsScored, ExtraType extra, boolean isWicket, Player nonStriker) {
            this.balledBy = balledBy;
            this.playedBy = playedBy;
            this.wicket = wicket;
            this.runsScored = runsScored;
            this.extra = extra;
            this.isWicket = isWicket;
            this.nonStriker = nonStriker;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isLegalDelivery() {
            return extra != ExtraType.WIDE && extra != ExtraType.NO_BALL;
        }

        static class Builder {
            boolean isWicket;
            Player nonStriker;
            private Player balledBy;
            private Player playedBy;
            private Wicket wicket;
            private int runsScored;
            private ExtraType extra;

            public Builder balledBy(Player balledBy) {
                this.balledBy = balledBy;
                return this;
            }

            public Builder playedBy(Player playerBy) {
                this.playedBy = playerBy;
                return this;
            }

            public Builder wicket(Wicket wicket) {
                this.wicket = wicket;
                return this;
            }

            public Builder runsScored(int runsScored) {
                this.runsScored = runsScored;
                return this;
            }

            public Builder extra(ExtraType extra) {
                this.extra = extra;
                return this;
            }

            public Builder withNonStriker(Player nonStriker) {
                this.nonStriker = nonStriker;
                return this;
            }

            public Builder withIsWicket(boolean isWicket) {
                this.isWicket = isWicket;
                return this;
            }

            public Ball build() {
                return new Ball(
                        balledBy,
                        playedBy,
                        wicket,
                        runsScored,
                        extra,
                        isWicket,
                        nonStriker
                );
            }
        }
    }

    static class Innings {
        Team battingTeam;
        Team bowlingTeam;
        List<Ball> balls;
        int score;
        int wickets;
        int legalBalls;

        public Innings(Team battingTeam, Team bowlingTeam) {
            this.battingTeam = battingTeam;
            this.bowlingTeam = bowlingTeam;
            this.balls = new ArrayList<>();
        }

        public synchronized void addBall(Ball ball) {
            balls.add(ball);
            if (ball.isLegalDelivery()) legalBalls++;
        }

        public void addRuns(int run) {
            score += run;
        }

        public void addWicket() {
            wickets++;
        }

        public int getScore() {
            return score;
        }

        public int getWickets() {
            return wickets;
        }

        public synchronized int getLegalBalls() {
            return legalBalls;
        }

        public synchronized String getOvers() {
            return legalBalls / 6 + "." + legalBalls % 6;
        }

        @Override
        public String toString() {
            if (balls.isEmpty()) {
                return "Innings{" +
                        "battingTeam=" + battingTeam +
                        ", bowlingTeam=" + bowlingTeam +
                        ", score=" + score +
                        ", wickets=" + wickets +
                        '}';
            }
            Ball ball = balls.get(balls.size() - 1);
            return "Innings{" +
                    "battingTeam=" + battingTeam +
                    ", bowlingTeam=" + bowlingTeam +
                    ", batsmen=" + ball.playedBy +
                    ", non striker batsmen=" + ball.nonStriker +
                    ", baller=" + ball.balledBy +
                    (ball.isWicket ? ", wicket=" + ball.wicket : "") +
                    ", score=" + score +
                    ", wickets=" + wickets +
                    '}';
        }
    }

    static class LimitedOversFormat implements MatchFormat {
        private final int overs;

        public LimitedOversFormat(int overs) {
            this.overs = overs;
        }

        @Override
        public int getOvers() {
            return overs;
        }

        @Override
        public int noOfInnings() {
            return 2;
        }

        @Override
        public String getScore(Match match) {
            Innings inn = match.getInnings();
            if (inn == null) return "Match not started";
            String currScore = String.format("%s: %d/%d (%s ov) vs %s",
                    inn.battingTeam.name, inn.getScore(), inn.getWickets(), inn.getOvers(), inn.bowlingTeam.name);
            if (match.innings.size() > 1) {
                currScore += String.format(", target: %d", match.target() + 1);
            }
            return currScore;
        }
    }

    static class MatchScheduled implements MatchState {
        public static final MatchScheduled INSTANCE = new MatchScheduled();

        private MatchScheduled() {
        }
    }

    static class MatchStart implements MatchState {
        public static final MatchStart INSTANCE = new MatchStart();

        private MatchStart() {
        }

        @Override
        public String coinToss(Map<String, String> call, String coinFace) {
            for (Map.Entry<String, String> entry : call.entrySet()) {
                if (entry.getValue().equals(coinFace)) return entry.getKey();
            }
            throw new IllegalStateException("Next time make a call!");
        }
    }

    static class MatchLive implements MatchState {
        public static final MatchLive INSTANCE = new MatchLive();

        private MatchLive() {
        }

        @Override
        public Innings startInnings(Team batting, Team bowling) {
            return new Innings(batting, bowling);
        }

        @Override
        public void updateBall(Ball ball, Innings inn) {
            inn.addBall(ball);
            inn.addRuns(ball.runsScored);
            if (ball.isWicket) inn.addWicket();
        }

        @Override
        public boolean hasMatchEnded(Match match) {
            List<Innings> inns = match.innings;
            if (inns.size() < match.matchFormat.noOfInnings()) return false;

            Innings last = inns.get(inns.size() - 1);

            boolean allOut = last.getWickets() == 10;
            boolean oversDone = last.getLegalBalls() >= match.matchFormat.getOvers() * 6;
            boolean targetChased = match.isTargetChased();

            return allOut || oversDone || targetChased;
        }
    }

    static class MatchInBreak implements MatchState {
        public static final MatchInBreak INSTANCE = new MatchInBreak();

        private MatchInBreak() {
        }

        @Override
        public Innings nextInnings(Match match) {
            Innings inn = match.getInnings();
            int legalBalls = match.matchFormat.getOvers() * 6;
            if (inn.getWickets() == 10 || inn.getLegalBalls() >= legalBalls) {
                return new Innings(inn.bowlingTeam, inn.battingTeam);
            }
            throw new IllegalStateException("Innings not yet complete");
        }
    }

    static class MatchAbandoned implements MatchState {
        public static final MatchAbandoned INSTANCE = new MatchAbandoned();

        private MatchAbandoned() {
        }

        @Override
        public void setAbandoningReason(Match match, String reason) {
            match.setReason(reason);
        }
    }

    static class MatchFinished implements MatchState {
        public static final MatchFinished INSTANCE = new MatchFinished();

        private MatchFinished() {
        }

        @Override
        public Team winner(Match match) {
            List<Innings> inns = match.innings;
            if (inns.size() == match.matchFormat.noOfInnings()) {
                int scoreA = match.target(), scoreB = match.chase();
                if (scoreA > scoreB) return inns.get(0).battingTeam;
                else if (scoreA < scoreB) return inns.get(1).battingTeam;
            }
            return null;
        }
    }

    static class Match implements Observable {
        Team teamA, teamB;
        Optional<Team> winner;
        MatchFormat matchFormat;
        MatchState matchState;
        List<Innings> innings;
        Set<Observer> observers;
        String matchId;
        String abandonReason;

        public Match(String matchId, Team teamA, Team teamB, MatchFormat format) {
            this.matchId = matchId;
            this.teamA = teamA;
            this.teamB = teamB;
            this.matchFormat = format;
            this.matchState = MatchScheduled.INSTANCE;
            this.observers = ConcurrentHashMap.newKeySet();
            this.innings = new ArrayList<>();
            this.winner = Optional.empty();
        }

        public void setWinner(Team winner) {
            this.winner = Optional.ofNullable(winner);
        }

        public void addObserver(Observer observer) {
            this.observers.add(observer);
        }

        public void removeObserver(Observer observer) {
            this.observers.remove(observer);
        }

        public void start(Map<String, String> call, String coinFace) {
            if (!(this.matchState instanceof MatchScheduled))
                throw new IllegalStateException("incorrect match state " + this.matchState.getClass());
            this.matchState = MatchStart.INSTANCE;
            String team = this.matchState.coinToss(call, coinFace);
            this.matchState = MatchLive.INSTANCE;
            this.innings.add(team.equals(teamA.name) ? this.matchState.startInnings(teamA, teamB) : this.matchState.startInnings(teamB, teamA));
            notifyObservers(null);
        }

        public Innings getInnings() {
            return innings.isEmpty() ? null : innings.get(innings.size() - 1);
        }

        public synchronized void updateBall(Ball ball) {
            if (!(this.matchState instanceof MatchLive))
                throw new IllegalStateException("incorrect match state " + this.matchState.getClass());
            this.matchState.updateBall(ball, getInnings());
            if (this.matchState.hasMatchEnded(this)) {
                determineWinner();
            } else if (isInningsComplete()) {
                this.matchState = MatchInBreak.INSTANCE;
            }
            notifyObservers(ball);
        }

        private boolean isInningsComplete() {
            Innings inn = getInnings();
            return inn.getWickets() == 10 || inn.getLegalBalls() >= matchFormat.getOvers() * 6;
        }

        private void determineWinner() {
            this.matchState = MatchFinished.INSTANCE;
            setWinner(this.matchState.winner(this));
        }

        public Team getWinningTeam() {
            if (!(this.matchState instanceof MatchFinished))
                throw new IllegalStateException("incorrect match state " + this.matchState.getClass());
            return winner.orElse(null);
        }

        public String currScore() {
            return matchFormat.getScore(this);
        }

        public int target() {
            int target = 0;
            for (int i = 0; i < innings.size(); i++) {
                if (i % 2 == 0) target += innings.get(i).getScore();
            }
            return target;
        }

        public int chase() {
            int chase = 0;
            for (int i = 0; i < innings.size(); i++) {
                if (i % 2 != 0) chase += innings.get(i).getScore();
            }
            return chase;
        }

        public boolean isTargetChased() {
            return innings.size() > 1 && chase() > target();
        }

        public void startNextInnings() {
            if (!(this.matchState instanceof MatchInBreak))
                throw new IllegalStateException("incorrect match state " + this.matchState.getClass());
            this.innings.add(this.matchState.nextInnings(this));
            this.matchState = MatchLive.INSTANCE;
        }

        public void abandon(String reason) {
            this.matchState = MatchAbandoned.INSTANCE;
            this.matchState.setAbandoningReason(this, reason);
            notifyObservers(null);
        }

        public void setReason(String reason) {
            this.abandonReason = reason;
        }

        @Override
        public void notifyObservers(Ball ball) {
            for (Observer observer : observers) {
                observer.updateStateChange(this, ball);
            }
        }

        @Override
        public String toString() {
            return "Match{" +
                    "teamA=" + teamA.name +
                    ", teamB=" + teamB.name +
                    ", winner=" + winner.map(t -> t.name).orElse("none") +
                    ", matchFormat=" + matchFormat +
                    ", matchState=" + matchState.getClass().getSimpleName() +
                    (matchState instanceof MatchAbandoned ? ", abandonReason='" + abandonReason + '\'' : "") +
                    '}';
        }
    }

    static class User {
        String name;

        public User(String name) {
            this.name = name;
        }

        public void notify(Ball ball) {
            System.out.println(ball);
        }

        public void notify(String mssg) {
            System.out.println(mssg);
        }
    }

    static class UserService {
        private final NotifyUsers notifyUsers;

        public UserService(NotifyUsers notifyUsers) {
            this.notifyUsers = notifyUsers;
        }

        public User createUser(String name) {
            return new User(name);
        }

        public void registerUserIntoMatch(String matchId, User user) {
            notifyUsers.registerUser(matchId, user);
        }
    }

    static class PlayerObserver implements Observer {
        public static final PlayerObserver INSTANCE = new PlayerObserver();

        private PlayerObserver() {
        }

        @Override
        public void updateStateChange(Match match, Ball ball) {
            if (ball == null) return;
            updateBatsmenStats(ball.playedBy, ball);
            updateBowlerStats(ball.balledBy, ball);
        }

        private void updateBatsmenStats(Player player, Ball ball) {
            PlayerStats stats = player.getPlayerStats();
            if (ball.extra != ExtraType.WIDE) stats.setBallsPlayed(stats.getBallsPlayed() + 1);
            stats.setRunsScored(stats.getRunsScored() + ball.runsScored);
        }

        private void updateBowlerStats(Player player, Ball ball) {
            PlayerStats stats = player.getPlayerStats();
            if (ball.isWicket) stats.setWicketsTaken(stats.getWicketsTaken() + 1);
            if (ball.isLegalDelivery()) stats.setBallsBalled(stats.getBallsBalled() + 1);
            stats.setRunsConceded(stats.getRunsConceded() + ball.runsScored);
        }
    }

    static class TeamStatsObserver implements Observer {
        public static final TeamStatsObserver INSTANCE = new TeamStatsObserver();

        private TeamStatsObserver() {
        }

        @Override
        public void updateStateChange(Match match, Ball ball) {
            if (!(match.matchState instanceof MatchFinished)) return;
            TeamStats statsA = match.teamA.getTeamStats();
            TeamStats statsB = match.teamB.getTeamStats();
            statsA.setMatchesPlayed(statsA.getMatchesPlayed() + 1);
            statsB.setMatchesPlayed(statsB.getMatchesPlayed() + 1);
            Team winner = match.winner.orElse(null);
            if (winner == null) {
                statsA.setMatchesDrawn(statsA.getMatchesDrawn() + 1);
                statsB.setMatchesDrawn(statsB.getMatchesDrawn() + 1);
            } else {
                Team loser = winner == match.teamA ? match.teamB : match.teamA;
                winner.getTeamStats().setMatchesWon(winner.getTeamStats().getMatchesWon() + 1);
                loser.getTeamStats().setMatchesLost(loser.getTeamStats().getMatchesLost() + 1);
            }
        }
    }

    static class NotifyUsers implements Observer {
        public static final NotifyUsers INSTANCE = new NotifyUsers();

        private final Map<String, List<User>> users;

        private NotifyUsers() {
            users = new ConcurrentHashMap<>();
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
                Thread th = new Thread(runnable, "notify-users-cleaner");
                th.setDaemon(true);
                return th;
            });
            scheduler.scheduleAtFixedRate(this::clean, 60_000, 60_000, TimeUnit.MILLISECONDS);
        }

        public void registerUser(String matchId, User user) {
            users.compute(matchId, (k, v) -> {
                if (v == null) v = new ArrayList<>();
                v.add(user);
                return v;
            });
        }

        public void unregisterUser(String matchId, User user) {
            users.computeIfPresent(matchId, (k, v) -> {
                v.remove(user);
                return v;
            });
        }

        @Override
        public void updateStateChange(Match match, Ball ball) {
            users.computeIfPresent(match.matchId, (k, v) -> {
                for (User user : v) {
                    if (ball != null) {
                        user.notify(ball);
                    } else {
                        user.notify(match.toString());
                    }
                }
                return v;
            });
        }

        private void clean() {
            users.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }
    }

    static class DisplayScore implements Observer {
        public static final DisplayScore INSTANCE = new DisplayScore();

        private DisplayScore() {
        }

        @Override
        public void updateStateChange(Match match, Ball ball) {
            System.out.println(match.currScore());
        }
    }

    static class CommentaryService {
        public static final CommentaryService INSTANCE = new CommentaryService();

        private final Map<String, List<String>> comments;

        private CommentaryService() {
            comments = new ConcurrentHashMap<>();
        }

        public void addComment(String matchId, Ball ball) {
            comments.compute(matchId, (k, v) -> {
                if (v == null) v = new ArrayList<>();
                if (ball.isWicket) {
                    v.add(ball.wicket.toString());
                } else if (ball.extra != null) {
                    v.add(ball.balledBy + " " + ball.playedBy + " " + ball.extra.name() + " " + ball.runsScored);
                } else {
                    v.add(ball.balledBy + " " + ball.playedBy + " " + ball.runsScored);
                }
                return v;
            });
        }

        public List<String> getComments(String matchId) {
            return comments.getOrDefault(matchId, new ArrayList<>());
        }
    }

    static class CricInfoFacade {
        private static final Object lock = new Object();
        private static volatile CricInfoFacade instance = null;
        private final Map<String, Match> matches;
        private final CommentaryService commentaryService;
        private final UserService userService;

        private CricInfoFacade() {
            matches = new ConcurrentHashMap<>();
            commentaryService = CommentaryService.INSTANCE;
            userService = new UserService(NotifyUsers.INSTANCE);
        }

        public static CricInfoFacade getInstance() {
            if (instance == null) {
                synchronized (lock) {
                    if (instance == null) instance = new CricInfoFacade();
                }
            }
            return instance;
        }

        public Match createMatch(String matchId, Team a, Team b, MatchFormat format) {
            return matches.computeIfAbsent(matchId, k -> {
                Match match = new Match(matchId, a, b, format);
                match.addObserver(DisplayScore.INSTANCE);
                match.addObserver(PlayerObserver.INSTANCE);
                match.addObserver(TeamStatsObserver.INSTANCE);
                match.addObserver(NotifyUsers.INSTANCE);
                return match;
            });
        }

        public void startMatch(String matchId, Map<String, String> tossCalls, String coinFace) {
            getMatchOrThrow(matchId).start(tossCalls, coinFace);
        }

        public void startNextInnings(String matchId) {
            getMatchOrThrow(matchId).startNextInnings();
        }

        public void abandonMatch(String matchId, String reason) {
            getMatchOrThrow(matchId).abandon(reason);
        }

        public void processBall(String matchId, Player balledBy, Player playedBy, Player nonStriker,
                                int runs, ExtraType extraType, Wicket wicket) {
            Match match = getMatchOrThrow(matchId);
            Ball ball = Ball.builder()
                    .balledBy(balledBy)
                    .playedBy(playedBy)
                    .withNonStriker(nonStriker)
                    .runsScored(runs)
                    .extra(extraType)
                    .wicket(wicket)
                    .withIsWicket(wicket != null)
                    .build();
            match.updateBall(ball);
            commentaryService.addComment(matchId, ball);
        }

        public String getLiveScore(String matchId) {
            return getMatchOrThrow(matchId).currScore();
        }

        public List<String> getCommentary(String matchId) {
            return commentaryService.getComments(matchId);
        }

        public User createUser(String name) {
            return userService.createUser(name);
        }

        public void subscribeToMatch(String matchId, User user) {
            getMatchOrThrow(matchId);
            userService.registerUserIntoMatch(matchId, user);
        }

        public Team getWinner(String matchId) {
            return getMatchOrThrow(matchId).getWinningTeam();
        }

        public List<Match> getMatchHistory() {
            return matches.values().stream()
                    .filter(m -> m.matchState instanceof MatchFinished)
                    .toList();
        }

        public PlayerStats getPlayerStats(Player player) {
            return player.getPlayerStats();
        }

        private Match getMatchOrThrow(String matchId) {
            Match match = matches.get(matchId);
            if (match == null) throw new NoSuchElementException("No match with id " + matchId);
            return match;
        }
    }
}