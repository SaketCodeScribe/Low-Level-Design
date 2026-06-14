package Problems.ManagementStates.TaskManagementSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Functional requirements
 * 1. Teams can create tasks and assign or reassign them to individual users.
 * 2. Users can update the state of a task.
 * 3. The system must track audit history for every task, including user actions such as creation, assignment changes, and status changes.
 * 4. Task state changes should notify the team or relevant stakeholders.
 * 5. Each task can be assigned to only one user at a time.
 * 6. Task state transitions must follow valid rules, for example TODO -> IN_PROGRESS -> DONE.
 * 7. A completed task can be reopened only through an allowed transition, not arbitrarily.
 *
 * Non-functional requirements
 * 1. The code should be modular.
 * 2. The code should be extensible.
 * 3. The design should follow OOD principles for maintainability.
 * 4. The system should be thread-safe for concurrent access.
 * 5. The components should be testable in isolation.
 */
public class TaskManagementState {
    enum TaskStateType{
        ToDo,
        InProgress,
        Cancelled,
        Done;
    }

    static class Task{
        String taskId;
        String userId;
        TaskStateType stateType;
        ConcurrentLinkedQueue<Action> audits;

        public Task(String taskId) {
            this.taskId = taskId;
            stateType = null;
            audits = new ConcurrentLinkedQueue<>();
        }

        public void setUserId(String userId) {
            this.userId = userId;
            audit(new ChangeStateAction("Set User action", this));
        }

        private void audit(Action changeStateAction) {
            audits.offer(changeStateAction);
        }
        public void addComment(String comment, String userId){
            audit(new CommentAction(comment, userId));
        }

        public void setStateType(TaskStateType type){
            this.stateType = type;
            audit(new ChangeStateAction(type.toString(), this));
        }

        public void reset() {
            stateType = TaskStateType.ToDo;
            userId = "";
        }

        public void reOpen(){
            this.reset();
            audit(new ReOpenAction(taskId));
        }

        public void cancel() {
            stateType = TaskStateType.Cancelled;
            userId = "";
            audit(new CloseAction(taskId));
        }

        public List<Action> getAudits() {
            return audits.stream().toList();
        }
    }


    static interface Action{};
    static class CommentAction implements Action{
        String comment;
        String userId;

        public CommentAction(String comment, String userId) {
            this.comment = comment;
            this.userId = userId;
        }

        @Override
        public String toString() {
            return "CommentAction{" +
                    "comment='" + comment + '\'' +
                    ", userId='" + userId + '\'' +
                    '}';
        }
    }
    static class ChangeStateAction implements Action{
        Task task;
        String msg;

        public ChangeStateAction(String msg, Task task) {
            this.msg = msg;
            this.task = task;
        }

        @Override
        public String toString() {
            return "ChangeStateAction{" +
                    "task=" + task +
                    ", msg='" + msg + '\'' +
                    '}';
        }
    }
    static class ReOpenAction implements Action {
        String taskId;
        public ReOpenAction(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public String toString() {
            return "ReOpenAction{" +
                    "taskId='" + taskId + '\'' +
                    '}';
        }
    }

    static class CloseAction implements Action {
        String taskId;
        public CloseAction(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public String toString() {
            return "CloseAction{" +
                    "taskId='" + taskId + '\'' +
                    '}';
        }
    }
    static abstract class TaskState{
        Task task;

        public TaskState(Task task) {
            this.task = task;
        }
        abstract void assign(String userId);
        abstract void unAssign();
        abstract TaskState moveToInProgress();
        abstract TaskState moveToDone();
        abstract TaskState moveToToDo();
        abstract TaskState reOpen();
        abstract TaskState cancel();
    }

    static class ToDoState extends TaskState{
        public ToDoState(Task task) {
            super(task);
            this.task.setStateType(TaskStateType.ToDo);
        }

        @Override
        void assign(String userId) {
            task.setUserId(userId);
        }

        @Override
        void unAssign() {
            task.reset();
        }

        @Override
        TaskState moveToInProgress() {
            if (task.userId.isEmpty()) throw new RuntimeException("Task is not assigned to anyone!");
            return new InProgressState(task);
        }

        @Override
        TaskState moveToDone() {
            throw new RuntimeException("Task is in to do");
        }


        @Override
        TaskState moveToToDo() {
            throw new RuntimeException("Task is already in to do");
        }

        @Override
        TaskState reOpen() {
            throw new RuntimeException("Task is in to do");
        }

        @Override
        TaskState cancel() {
            return new CancelledState(task);
        }
    }
    static class InProgressState extends TaskState{
        public InProgressState(Task task) {
            super(task);
            this.task.setStateType(TaskStateType.InProgress);
        }

        @Override
        void assign(String userId) {
            throw new RuntimeException("Task state is in progress!");
        }

        @Override
        void unAssign() {
            throw new RuntimeException("Task state is in progress!");
        }

        @Override
        TaskState moveToInProgress() {
            throw new RuntimeException("Task state is in progress!");
        }

        @Override
        TaskState moveToDone() {
            return new DoneState(task);
        }

        @Override
        TaskState moveToToDo() {
            return new ToDoState(task);
        }

        @Override
        TaskState reOpen() {
            throw new RuntimeException("Task state is in progress!");
        }

        @Override
        TaskState cancel() {
            throw new RuntimeException("Task is in progress");

        }
    }
    static class DoneState extends TaskState{
        public DoneState(Task task) {
            super(task);
            task.stateType = TaskStateType.Done;
        }

        @Override
        void assign(String userId) {
            throw new RuntimeException("Task state is in Done!");
        }

        @Override
        void unAssign() {
            throw new RuntimeException("Task state is in Done!");

        }

        @Override
        TaskState moveToInProgress() {
            return new InProgressState(task);
        }

        @Override
        TaskState moveToDone() {
            throw new RuntimeException("Task state is in Done!");
        }

        @Override
        TaskState moveToToDo() {
            throw new RuntimeException("Task state is in Done!");
        }

        @Override
        TaskState reOpen() {
            task.reOpen();
            return new ToDoState(task);
        }

        @Override
        TaskState cancel() {
            task.reset();
            return new CancelledState(task);
        }
    }
    static class CancelledState extends TaskState{
        public CancelledState(Task task) {
            super(task);
            task.setStateType(TaskStateType.Cancelled);
        }

        @Override
        void assign(String userId) {
            throw new RuntimeException("Task is already in cancelled state");
        }

        @Override
        void unAssign() {
            throw new RuntimeException("Task is already in cancelled state");
        }

        @Override
        TaskState moveToInProgress() {
            throw new RuntimeException("Task is already in cancelled state");
        }

        @Override
        TaskState moveToDone() {
            throw new RuntimeException("Task is already in cancelled state");
        }

        @Override
        TaskState moveToToDo() {
            throw new RuntimeException("Task is already in cancelled state");
        }

        @Override
        TaskState reOpen() {
            task.reOpen();
            return new ToDoState(task);
        }

        @Override
        TaskState cancel() {
            throw new RuntimeException("Task is already in cancelled state");
        }
    }
    static class User{
        String userId;
        String teamId;
        ConcurrentLinkedQueue<Action> audits;
        Map<String, TaskState> userTasks;

        public User(String userId, String teamId) {
            this.teamId = teamId;
            this.userId = userId;
            audits = new ConcurrentLinkedQueue<>();
            userTasks = new HashMap<>();
        }
        public void changeState(TaskController controller, UserAction action, String taskId){
            userTasks.computeIfPresent(taskId, (k,v) -> {
                if (v != null){
                    v = controller.action(v, action, userId);
                    audit(new ChangeStateAction("Removed", v.task));
                }
                return v;
            });
            if (action == UserAction.Cancel){
                userTasks.remove(taskId);
            }
        }
        public void addComment(TaskController controller, Task task, String comment){
            controller.addComment(task, UserAction.Comment, comment, userId);
        }
        public void remove(String taskId){
            audit(new ChangeStateAction("Removed", userTasks.remove(taskId).task));
        }

        private void audit(Action changeStateAction) {
            audits.offer(changeStateAction);
        }

    }
    static class Team{
        String teamId;
        Set<TaskState> tasks;
        Set<User> users;
        ConcurrentHashMap<String, User> taskToUser;

        public Team(String id) {
            teamId = id;
            tasks = ConcurrentHashMap.newKeySet();
            users = ConcurrentHashMap.newKeySet();
        }

        public void addUser(User user){
            users.add(user);
        }

        public void addTask(TaskState task){
            tasks.add(task);
        }

        public void removeUser(User user){
            users.remove(user);
        }
        public TaskState removeTask(TaskState task){
            tasks.remove(task);
            return task;
        }

    }

    enum UserAction{
        Assign,
        Cancel,
        Reopen,
        InProgress,
        Comment,
        Done;
    }
    static class TaskController{
        public TaskState action(TaskState task, UserAction action, String userId) {
            switch (action){
                case Assign -> task.assign(userId);
                case InProgress -> task = task.moveToInProgress();
                case Done -> task = task.moveToDone();
                case Cancel -> task = task.cancel();
                case Reopen -> task = task.reOpen();
                default -> throw new RuntimeException("Invalid Action");
            }
            return task;
        }

        public Task createTask(String taskId){
            return new Task(taskId);
        }

        public void addComment(Task task, UserAction userAction, String comment, String userId) {
            if (userAction != UserAction.Comment) throw new RuntimeException("Invalid action!");
            task.addComment(comment, userId);
        }
    }

    Map<String, Team> teams;
    Map<String, TaskState> taskStates;
    TaskController controller;

    public TaskManagementState(Map<String, Team> teams, Map<String, TaskState> taskStates, TaskController controller) {
        this.teams = teams;
        this.taskStates = taskStates;
        this.controller = controller;
    }

    public void createTask(String teamId, String taskId){
        teams.computeIfPresent(teamId, (k,v) -> {
            v.addTask(taskStates.computeIfAbsent(taskId, x -> new ToDoState(new Task(taskId))));
            return v;
        });
    }

    public void cancelTask(String teamId, String taskId){
        teams.computeIfPresent(teamId, (k,v) -> {
            var task = v.removeTask(taskStates.get(taskId));
            controller.action(task, UserAction.Cancel, task.task.userId);
            return v;
        });
    }

    public void assign(String taskId, String userId){
        controller.action(taskStates.get(taskId), UserAction.Assign, userId);
    }

    public void addComment(String taskId, String comment, String userId){
        controller.addComment(taskStates.get(taskId).task, UserAction.Comment, comment, userId);
    }

    public void action(User user, String taskId, UserAction action){
        user.changeState(controller, action, taskId);
    }
}
