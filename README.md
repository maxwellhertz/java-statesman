A minimal library for working with state machines in Java applications. Inspired by [statesman](https://github.com/gocardless/statesman).

# Quick Start

Let's say you have a `Payment` class like this:

```java
public class Payment {
    private Long id;
    private Long amount;
    ...
}
```

and a payment might be in one of these states:

```java
public enum PaymentState {
    PENDING(1),
    CONFIRMED(2),
    PAID(3),
    CANCELLED(4);
    ...
}
```

Now you can set up a state machine easily:

```java
// pending -> [confirmed, cancelled]
// confirmed -> [paid]
final var transitionRules = Map.of(
        PaymentState.PENDING, Set.of(PaymentState.CONFIRMED, PaymentState.CANCELLED),
        PaymentState.CONFIRMED, Set.of(PaymentState.CANCELLED)
);
final var stateMachine = new StateMachine<Payment, PaymentState>(
        PaymentState.PENDING,
        transitionRules,
        new PaymentStateTransitionRepository()
);
```

The constructor of `StateMachine` takes 3 parameters:

1. The initial state of the machine
2. a collection of transition rules, and
3. a repository used to query and persist transition records.

You can also provide some additional guard functions that will work with pre-defined transition rules to determine whether a transition is allowed:

```java
// A payment will be confirmed only when its amount is greater than 0.
stateMachine.addTransitionGuard(
        PaymentState.PENDING, 
        PaymentState.CONFIRMED, 
        payment -> Long.valueOf(0).compareTo(payment.getAmount()) < 0
);
```

If you have some work to be done before and/or after a transition, use callbacks:

```java
// A payment should be persisted in the data store before it is confirmed.
final var paymentRepo = new PaymentRepository();
stateMachine.addTransitionCallback(
        PaymentState.PENDING, 
        PaymentState.CONFIRMED, 
        StateMachine.CallbackType.BEFORE, 
        paymentRepo::create
);
```