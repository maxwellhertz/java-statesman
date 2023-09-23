package maxwellhertz.statesman.core;

import java.util.Optional;

/**
 * This interface provides methods to query and persist transition records.
 */
public interface StateTransitionRepository<Model, State> {
    /**
     * Get the current state of a model.
     *
     * @param model the data model
     * @return the current state (which might be null if the state machine has not performed any transitions)
     * @throws IllegalArgumentException model must not be null
     */
    Optional<State> getLatest(final Model model) throws IllegalArgumentException;

    /**
     * Record a state transition.
     *
     * @param model the data model
     * @param from the initial state
     * @param to the target state
     * @throws IllegalArgumentException none of these arguments can be null
     */
    void create(final Model model, final State from, final State to) throws IllegalArgumentException;
}
