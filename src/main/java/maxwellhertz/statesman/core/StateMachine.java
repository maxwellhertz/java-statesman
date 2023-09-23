package maxwellhertz.statesman.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This is the core model.
 */
public class StateMachine<Model, State> {
    private final State initialState;

    private final Map<State, Set<State>> transitionRules;

    private final StateTransitionRepository<Model, State> stateTransitionRepo;

    private final Map<Pair<State>, Predicate<Model>> transitionGuards;

    private final Map<Pair<State>, Consumer<Model>> beforeCallbacks;

    private final Map<Pair<State>, Consumer<Model>> afterCallbacks;

    /**
     * Create a state machine.
     *
     * @param initialState        the initial state
     * @param transitionRules     each key-value pair specifies which states the state machine can transition to from the given state
     * @param stateTransitionRepo used to query and persist records of state transitions
     * @throws IllegalArgumentException initialState and stateTransitionRepo must not be null
     */
    public StateMachine(final State initialState,
                        final Map<State, Set<State>> transitionRules,
                        final StateTransitionRepository<Model, State> stateTransitionRepo) throws IllegalArgumentException {
        if (initialState == null) {
            throw new IllegalArgumentException("the initial state must be provided");
        }
        if (stateTransitionRepo == null) {
            throw new IllegalArgumentException("stateTransitionRepo must not be null");
        }

        this.initialState = initialState;
        this.transitionRules = transitionRules == null ? Collections.emptyMap() : transitionRules;
        this.transitionGuards = new ConcurrentHashMap<>();
        this.beforeCallbacks = new ConcurrentHashMap<>();
        this.afterCallbacks = new ConcurrentHashMap<>();
        this.stateTransitionRepo = stateTransitionRepo;
    }

    /**
     * Query the current state of a model.
     *
     * @param model the data model
     * @return the current state
     */
    public State currentState(final Model model) {
        return stateTransitionRepo.getLatest(model).orElse(initialState);
    }

    /**
     * Add a guard function for a transition.
     *
     * @param from  the initial state
     * @param to    the target state
     * @param guard a function that is called to determine whether the transition is allowed
     * @throws IllegalArgumentException all arguments must not be null
     */
    public void addTransitionGuard(final State from, final State to, Predicate<Model> guard)
            throws IllegalArgumentException {
        if (from == null || to == null || guard == null) {
            throw new IllegalArgumentException("all arguments must not be null");
        }
        transitionGuards.put(new Pair<>(from, to), guard);
    }

    /**
     * Add a callback function for a transition
     *
     * @param from the initial state
     * @param to the target state
     * @param callbackType when to execute the callback
     * @param callback the callback function
     * @throws IllegalArgumentException all arguments must not be null
     */
    public void addTransitionCallback(final State from,
                                      final State to,
                                      final CallbackType callbackType,
                                      final Consumer<Model> callback) {
        if (from == null || to == null || callbackType == null || callback == null) {
            throw new IllegalArgumentException("all arguments must not be null");
        }

        final Pair<State> pair = new Pair<>(from, to);
        if (callbackType == CallbackType.BEFORE) {
            beforeCallbacks.put(pair, callback);
        } else {
            afterCallbacks.put(pair, callback);
        }
    }

    /**
     * Test whether a transition is valid.
     *
     * @param model the data model
     * @param target the target state
     * @return true if the state machine can transition target the target state
     * @throws IllegalArgumentException target must not be null
     */
    public boolean canTransitionTo(final Model model, final State target) throws IllegalArgumentException {
        if (target == null) {
            throw new IllegalArgumentException("the target state must not be null");
        }

        final State currentState = currentState(model);
        if (!transitionRules.getOrDefault(currentState, Collections.emptySet()).contains(target)) {
            return false;
        }
        final Predicate<Model> guard = transitionGuards.get(new Pair<>(currentState, target));
        return guard == null || guard.test(model);
    }

    /**
     * Make the state machine transition target the target state.
     *
     * @param model the data model
     * @param target the target state
     * @throws IllegalArgumentException target must not be null
     * @throws IllegalStateException    if the transition is not allowed
     */
    public void transitionTo(final Model model, final State target) throws IllegalArgumentException, IllegalStateException {
        if (target == null) {
            throw new IllegalArgumentException("the target state must not be null");
        }
        final State currentState = currentState(model);
        if (!canTransitionTo(model, target)) {
            throw new IllegalStateException(String.format(
                    "the model (%s) can not transition from %s to %s",
                    model.toString(),
                    currentState.toString(),
                    target.toString()
            ));
        }

        final Pair<State> statePair = new Pair<>(currentState, target);
        if (beforeCallbacks.containsKey(statePair)) {
            beforeCallbacks.get(statePair).accept(model);
        }
        stateTransitionRepo.create(model, currentState, target);
        if (afterCallbacks.containsKey(statePair)) {
            afterCallbacks.get(statePair).accept(model);
        }
    }

    public enum CallbackType {
        BEFORE, AFTER;
    }

    private static class Pair<State> {
        private final State from;

        private final State to;

        private Pair(State from, State to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pair<?> pair = (Pair<?>) o;
            return Objects.equals(from, pair.from) && Objects.equals(to, pair.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }
}
