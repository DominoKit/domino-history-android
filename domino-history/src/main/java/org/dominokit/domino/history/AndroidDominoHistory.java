package org.dominokit.domino.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class AndroidDominoHistory implements AppHistory {

    private Set<HistoryListener> listeners = new HashSet<>();
    private Deque<HistoryState> forwards = new LinkedList<>();
    private Deque<HistoryState> backwards = new LinkedList<>();

    @Override
    public DirectState listen(StateListener listener) {
        return listen(TokenFilter.any(), listener, false);
    }

    @Override
    public DirectState listen(TokenFilter tokenFilter, StateListener listener) {
        return listen(tokenFilter, listener, false);
    }

    @Override
    public DirectState listen(StateListener listener, boolean removeOnComplete) {
        return listen(TokenFilter.any(), listener, removeOnComplete);
    }

    @Override
    public DirectState listen(TokenFilter tokenFilter, StateListener listener, boolean removeOnComplete) {
        listeners.add(new HistoryListener(listener, tokenFilter, removeOnComplete));
        return new DominoDirectState(tokenFilter, currentState(), listener);
    }

    @Override
    public void removeListener(StateListener listener) {
        listeners = listeners.stream()
                .filter(historyListener -> !historyListener.listener.equals(listener))
                .collect(Collectors.toSet());
    }

    private State currentState() {
        if (forwards.isEmpty())
            return new TestState(nullState());
        return new TestState(forwards.peek());
    }

    private HistoryState nullState() {
        return new HistoryState("", "");
    }

    private void inform(HistoryState state) {
        List<HistoryListener> completedListeners = new ArrayList<>();
        listeners.stream()
                .filter(l -> {
                    NormalizedToken normalized = getNormalizedToken(state.token, l);
                    if (isNull(normalized)) {
                        normalized = new DefaultNormalizedToken(state.token);
                    }
                    return l.tokenFilter.filter(new TestState(new HistoryState(normalized.getToken().value(), "test")).token());
                })
                .forEach(l -> {
                    if (l.isRemoveOnComplete()) {
                        completedListeners.add(l);
                    }

                    NormalizedToken normalized = getNormalizedToken(state.token, l);
                    l.listener.onPopState(new TestState(normalized, new HistoryState(normalized.getToken().value(), "test")));
                });

        listeners.removeAll(completedListeners);
    }

    private NormalizedToken getNormalizedToken(String token, HistoryListener listener) {
        return listener.tokenFilter.normalizeToken(token);
    }

    @Override
    public void back() {
        if (!backwards.isEmpty()) {
            final HistoryState state = backwards.pop();
            forwards.push(state);
            if (!backwards.isEmpty()) {
                inform(backwards.peek());
            }
        }
    }

    @Override
    public void forward() {
        if (!forwards.isEmpty()) {
            final HistoryState state = forwards.pop();
            backwards.push(state);
            inform(state);
        }
    }

    @Override
    public void pushState(String token, String title, String data) {
        push(token, data);
    }

    @Override
    public void pushState(String token, String title, String data, TokenParameter... parameters) {
        push(token, data, parameters);
    }

    @Override
    public void pushState(String token) {
        push(token, "");
    }

    @Override
    public void pushState(String token, TokenParameter... parameters) {
        push(token, "", parameters);
    }

    @Override
    public void fireState(String token, String title, String data) {
        fireState(token, title, data, new TokenParameter[0]);
    }

    @Override
    public void fireState(String token, String title, String data, TokenParameter... parameters) {
        pushState(token, title, data, parameters);
        fireCurrentStateHistory();
    }

    @Override
    public void fireState(String token) {
        fireState(token, new TokenParameter[0]);
    }

    @Override
    public void fireState(String token, TokenParameter... parameters) {
        pushState(token, parameters);
        fireCurrentStateHistory();
    }

    @Override
    public void replaceState(String token, String title, String data) {
        backwards.pop();
        push(token, data);
    }

    @Override
    public HistoryToken currentToken() {
        if (backwards.isEmpty())
            return new StateHistoryToken("");
        return new StateHistoryToken(requireNonNull(backwards.peek()).token);
    }

    @Override
    public void fireCurrentStateHistory() {
        if (!backwards.isEmpty())
            inform(backwards.peek());
    }

    public void initialState(String token, String data) {
        push(token, data);
    }

    private void push(String token, String data, TokenParameter... parameters) {
        HistoryState state = new HistoryState(replaceParameters(token, Arrays.asList(parameters)), data);
        if (!backwards.contains(state))
            backwards.push(state);
    }

    private String replaceParameters(String token, List<TokenParameter> parametersList) {
        String result = token;
        for (TokenParameter parameter : parametersList) {
            result = result.replace(":" + parameter.getName(), parameter.getValue());
        }
        return result;
    }

    private class HistoryListener {
        private final StateListener listener;
        private final TokenFilter tokenFilter;
        private final boolean removeOnComplete;

        public HistoryListener(StateListener listener, TokenFilter tokenFilter, boolean removeOnComplete) {
            this.listener = listener;
            this.tokenFilter = tokenFilter;
            this.removeOnComplete = removeOnComplete;
        }

        public boolean isRemoveOnComplete() {
            return removeOnComplete;
        }
    }

    public Set<HistoryListener> getListeners() {
        return listeners;
    }

    public Deque<HistoryState> getForwards() {
        return forwards;
    }

    public Deque<HistoryState> getBackwards() {
        return backwards;
    }

    private class TestState implements State {

        private final HistoryState historyState;
        private NormalizedToken normalizedToken;

        private TestState(HistoryState historyState) {
            this.historyState = historyState;
        }

        private TestState(NormalizedToken normalizedToken, HistoryState historyState) {
            this.normalizedToken = normalizedToken;
            this.historyState = historyState;
        }

        @Override
        public HistoryToken token() {
            return new StateHistoryToken(historyState.token);
        }

        @Override
        public String data() {
            return historyState.data;
        }

        @Override
        public String title() {
            return "test title";
        }

        @Override
        public NormalizedToken normalizedToken() {
            return normalizedToken;
        }

        @Override
        public void setNormalizedToken(NormalizedToken normalizedToken) {
            this.normalizedToken = normalizedToken;
        }
    }

    public class HistoryState {
        private final String token;
        private final String data;

        public HistoryState(String token, String data) {
            this.token = token;
            this.data = data;
        }

        public String getToken() {
            return token;
        }

        public String getData() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HistoryState that = (HistoryState) o;
            return Objects.equals(token, that.token);
        }

        @Override
        public int hashCode() {
            return Objects.hash(token);
        }
    }
}
