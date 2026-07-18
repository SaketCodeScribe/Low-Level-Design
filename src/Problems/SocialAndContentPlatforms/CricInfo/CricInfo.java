package Problems.SocialAndContentPlatforms.CricInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FRs:
 * 1. System supports multiple matches
 * 2. Match type - 20 ov, 50 ov, test(4 day match)
 * 3. Live score with current batter, batsmen at non side and current bowler, bally by ball commentary
 * 4. Live feature and autoPlay feature of match
 * 5. past match history, player stats.
 * 7. data is coming ball by ball from an api into system
 *
 * NFRs:
 * 1. Modular: system should be composed of well-separated components
 * 2. Extensibility: system should be able to support future features
 * 3. Maintainability: system should follow OOD for clean, smaller and east to test code
 */
public class CricInfo {
    enum MatchStatus {
        NOT_STARTED,
        LIVE,
        FINISHED,
        DRAW,
        IN_BREAK,
        ABANDONED;
    }
    enum PlayerRole{
        BATSMAN,
        BOWLER,
        ALL_ROUNDER;
    }
    enum WicketType{
        LBW,
        RUN_OUT,
        CATCH_OUT,
        STUMPED,
        BOLD;
    }
    enum ExtraType{
        WIDE,
        NO_BALL,
        BYE,
        LEG_BYE
    }
    enum MatchFormat{
        OVER_20,
        OVER_50,
        TEST;
    }
    static class PlayerStats{
        int runsScored;
        int ballsPlayed;
        int wicketsTaken;
        int runsConceded;
        int ballsBowled;

        public int getRunsScored() {
            return runsScored;
        }

        public void setRunsScored(int runsScored) {
            this.runsScored = runsScored;
        }

        public int getBallsPlayed() {
            return ballsPlayed;
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

        public int getBallsBowled() {
            return ballsBowled;
        }

        public void setBallsBowled(int ballsBowled) {
            this.ballsBowled = ballsBowled;
        }
    }
    static class Player{
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
    static class TeamStats{
        int matchesWon;
        int matchesLost;
        int matchesDrawn;
        int matchesPlayed;

        public TeamStats() {
        }

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
    static class Team{
        String name;
        Set<Player> players;
        TeamStats teamStats;

        public Team(String name) {
            this.name = name;
            this.players = new HashSet<>();
            teamStats = new TeamStats();
        }
        public void addPlayer(Player player){
            players.add(player);
        }
        public void removePlayer(Player player){
            players.remove(player);
        }

        public TeamStats getTeamStats() {
            return teamStats;
        }
    }
    static class Wicket{
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

        public static Builder builder(){
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

        static class Builder{
            WicketType wicketType;
            Player playerOut;
            Player balledBy;
            Player caughtBy;
            Player runOutBy;
            Player stumpedBy;

            public Builder() {
            }

            public Builder withWicketType(WicketType wicketType){
                this.wicketType = wicketType;
                return this;
            }
            public Builder withPlayerOut(Player player){
                this.playerOut = player;
                return this;
            }
            public Builder withBalledBy(Player player){
                this.balledBy = player;
                return this;
            }
            public Builder withCaughtBy(Player player){
                this.caughtBy = player;
                return this;
            }
            public Builder withRunOutBy(Player player){
                this.runOutBy = player;
                return this;
            }
            public Builder withStumpedBy(Player player){
                this.stumpedBy = player;
                return this;
            }
            public Wicket build(){
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

        static class Builder {
            private Player balledBy;
            private Player playedBy;
            private Wicket wicket;
            private int runsScored;
            private ExtraType extra;
            boolean isWicket;
            Player nonStriker;

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

            public Builder withNonStriker(Player nonStriker){
                this.nonStriker = nonStriker;
                return this;
            }

            public Builder withIsWicket(boolean isWicket){
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
    static class Innings{
        Team battingTeam;
        Team bowlingTeam;
        List<Ball> balls;
        int score;
        int wickets;
        int days;
        Player baller;

        public Innings(Team battingTeam, Team bowlingTeam) {
            this.battingTeam = battingTeam;
            this.bowlingTeam = bowlingTeam;
            this.balls = new ArrayList<>();
        }
        public synchronized void addBall(Ball ball){
            balls.add(ball);
        }
        public void addRuns(int run){
            score += run;
        }
        public void addWicket(){
            wickets++;
        }
        public int getScore() {
            return score;
        }

        public int getWickets() {
            return wickets;
        }
        public synchronized String getOvers(){
            return balls.size()/6 + "." + balls.size()%6;
        }

        @Override
        public String toString() {
            Ball ball = balls.get(balls.size() - 1);
            return "Innings{" +
                    "battingTeam=" + battingTeam +
                    ", bowlingTeam=" + bowlingTeam +
                    ", batsmen=" + ball.playedBy+
                    ", non striker batsmen=" + ball.nonStriker+
                    ", baller=" + ball.balledBy+
                    (ball.isWicket ? ", wicket=" + ball.wicket : "")+
                    ", score=" + score +
                    ", wickets=" + wickets +
                    ", days=" + days +
                    '}';
        }
    }
    static interface Observer{
        public void updateStateChange(Object object);
    }
    static interface Observable{
        public void notifyObservers();
    }
    static class Match implements  Observable{
        Team teamA, teamB;
        MatchStatus matchStatus;
        Team winner;
        MatchFormat matchFormat;
        List<Innings> innings;
        Set<Observer> observers;
        Date date;
        String matchId;

        public Match(String matchId, Team teamA, Team teamB, MatchFormat format, Date date) {
            this.matchId = matchId;
            this.teamA = teamA;
            this.teamB = teamB;
            this.matchStatus = MatchStatus.NOT_STARTED;
            this.matchFormat = format;
            this.date = date;
            this.observers = ConcurrentHashMap.newKeySet();
            this.innings = new ArrayList<>();
        }

        public void setWinner(Team winner) {
            this.winner = winner;
        }

        public void setMatchStatus(MatchStatus matchStatus) {
            this.matchStatus = matchStatus;
        }

        public void addObserver(Observer observer){
            this.observers.add(observer);
        }
        public void removeObserver(Observer observer){
            this.observers.remove(observer);
        }

        public void createInning(Team batting, Team bowling){
            innings.add(new Innings(batting, bowling));
            if (MatchFormat.TEST == matchFormat && innings.size() > 1) {
                getInnings().days = innings.get(innings.size()-2).days;
            }
        }

        public void increaseDay(){
            if (Objects.requireNonNull(this.matchFormat) == MatchFormat.TEST) {
                getInnings().days++;
            } else {
                throw new IllegalStateException("incorrect match format " + this.matchFormat);
            }
        }
        public Innings getInnings(){
            return innings.isEmpty() ? null : innings.get(innings.size()-1);
        }
        public void playBall(Ball ball){
            if (matchStatus != MatchStatus.LIVE) return;
            Innings inn = getInnings();
            if (inn == null) return;
            inn.addBall(ball);
            if (!ball.isWicket)
                inn.addRuns(ball.runsScored);
            else inn.addWicket();
            switch (matchFormat){
                case OVER_20 , OVER_50 ->  {
                    if (innings.size() == 2) this.matchStatus = MatchStatus.FINISHED;
                }
                case TEST -> {
                    if (innings.size() == 4 || getInnings().days == 5) this.matchStatus = MatchStatus.FINISHED;
                }
                default -> throw new IllegalStateException("incorrect match format "+this.matchFormat);
            }
            if (matchStatus == MatchStatus.FINISHED) determineWinner();
            notifyObservers();
        }

        private void determineWinner() {
            switch (matchFormat){
                case OVER_20 , OVER_50 -> {
                    if (innings.get(0).score > innings.get(1).score){
                        setWinner(innings.get(0).battingTeam);
                    }
                    else if (innings.get(0).score < innings.get(1).score){
                        setWinner(innings.get(1).battingTeam);
                    }
                    else this.matchStatus = MatchStatus.DRAW;
                }
                case TEST -> {
                    if (innings.get(innings.size()-1).days == 5 && innings.size() < 4) this.matchStatus = MatchStatus.DRAW;
                    int scoreA = innings.get(0).getScore()+innings.get(2).getScore();
                    int scoreB = innings.get(1).getScore()+innings.get(3).getScore();
                    if (scoreA > scoreB){
                        setWinner(innings.get(0).battingTeam);
                    } else if (scoreA < scoreB) {
                        setWinner(innings.get(1).battingTeam);
                    } else this.matchStatus = MatchStatus.DRAW;
                }
                default -> throw new IllegalStateException("incorrect match format "+this.matchFormat);
            }
        }

        @Override
        public void notifyObservers() {
            for(Observer observer:observers){
                observer.updateStateChange(this);
            }
        }
    }
    static class PlayerObserver implements Observer{
        @Override
        public void updateStateChange(Object object) {
            if (!(object instanceof Ball ball)) return;
            Player batsmen = ball.playedBy;
            Player baller = ball.balledBy;
            int run = ball.runsScored;
            boolean isWicket = ball.isWicket;
            PlayerStats ballerStats = baller.getPlayerStats();
            PlayerStats batsmenStats = batsmen.getPlayerStats();

            if (isWicket){
                ballerStats.setWicketsTaken(ballerStats.getWicketsTaken()+1);
            }
            else {
                batsmenStats.setRunsScored(ballerStats.getRunsScored()+run);
            }
            ballerStats.setBallsBowled(ballerStats.getBallsBowled()+1);
            batsmenStats.setBallsBowled(ballerStats.getBallsBowled()+1);
            ballerStats.setRunsConceded(ballerStats.getRunsConceded()+run);
        }
    }

    static class DisplayScore implements Observer{
        @Override
        public void updateStateChange(Object object) {
            if (!(object instanceof Match match)) return;
            System.out.println(match.getInnings());
        }
    }

    static class CommentaryService{
        Map<String, List<String>> comments;

        public CommentaryService() {
            comments = new ConcurrentHashMap<>();
        }

        public void addComment(String matchId, Ball ball){
            comments.compute(matchId, (k,v) -> {
                if (v == null) return new ArrayList<>();
                ExtraType extraType = ball.extra;
                boolean isWicket = ball.isWicket;
                if (isWicket){
                    v.add(ball.wicket.toString());
                }
                else if (extraType != null){
                    v.add(ball.balledBy+" "+ball.playedBy+" "+extraType.name()+ " "+ball.runsScored);
                }
                else v.add(ball.balledBy+" "+ball.playedBy+" "+ball.runsScored);
                return v;
            });
        }

        public List<String> getComments(String matchId){
            return comments.getOrDefault(matchId, new ArrayList<>());
        }

    }

    static class CricInfoFacade{
        private static volatile CricInfoFacade instance = null;
        private static Object lock = new Object();
        private Map<String, Match> matches;
        private final CommentaryService commentaryService;
        private final PlayerObserver playerObserver;


        public CricInfoFacade() {
            matches = new ConcurrentHashMap<>();
            commentaryService = new CommentaryService();
            playerObserver = new PlayerObserver();
        }

        public static CricInfoFacade getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new CricInfoFacade();
                    }
                }
            }
            return instance;
        }

        public Match createMatch(String matchId, Team a, Team b, MatchFormat matchFormat, Date date){
            return matches.computeIfAbsent(matchId, k -> {
                Match match = new Match(matchId, a, b, matchFormat, date);
                match.addObserver(new DisplayScore());
                match.addObserver(playerObserver);
                return match;
            });
        }

        public void startInnings(String matchId, Team a, Team b){
            matches.computeIfPresent(matchId, (k, v) -> {
                v.createInning(a, b);
                return v;
            });
        }

        public void playBall(Player balledBy, Player playedBy, Player nonStriker, int runs, ExtraType extraType, Wicket wicket, String matchId) {
            Match match = matches.computeIfPresent(matchId, (k, v) -> {
                Ball ball = Ball.builder()
                        .balledBy(balledBy)
                        .playedBy(playedBy)
                        .runsScored(runs)
                        .extra(extraType)
                        .withIsWicket(wicket != null)
                        .wicket(wicket)
                        .withNonStriker(nonStriker)
                        .build();
                v.playBall(ball);
                commentaryService.addComment(matchId, ball);
                return v;
            });
        }

        public List<String> getComments(String matchId){
            return commentaryService.getComments(matchId);
        }

        public void winnerOfMatch(String matchId){
            matches.computeIfPresent(matchId, (k, v) -> {
                if (v.matchStatus != MatchStatus.FINISHED) {
                    System.out.println(matchId + " "+v.matchStatus);
                }
                System.out.println(v.winner);
                return v;
            });
        }

    }
}
