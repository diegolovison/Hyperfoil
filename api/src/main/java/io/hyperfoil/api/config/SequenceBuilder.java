/*
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.hyperfoil.api.config;

import java.util.ArrayList;
import java.util.Objects;

import io.hyperfoil.function.SerializableSupplier;
import io.hyperfoil.impl.FutureSupplier;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class SequenceBuilder extends BaseSequenceBuilder {
    private final ScenarioBuilder scenario;
    private final String name;
    private int id;
    private Sequence sequence;
    // Next sequence as set by parser. It's not possible to add this as nextSequence step
    // since that would break anchors - we can insert it only after parsing is complete.
    private String nextSequence;

    SequenceBuilder(ScenarioBuilder scenario, String name) {
        super(null);
        this.scenario = scenario;
        this.name = Objects.requireNonNull(name);
    }

    SequenceBuilder(ScenarioBuilder scenario, SequenceBuilder other) {
        super(null);
        this.scenario = scenario;
        this.name = other.name;
        readFrom(other);
        this.nextSequence = other.nextSequence;
    }

    public void prepareBuild() {
        // capture local var to prevent SequenceBuilder serialization
        String nextSequence = this.nextSequence;
        if (nextSequence != null) {
            step(s -> {
                s.nextSequence(nextSequence);
                return true;
            });
        }
        // We need to make a defensive copy as prepareBuild() may trigger modifications
        new ArrayList<>(steps).forEach(StepBuilder::prepareBuild);
    }

    public Sequence build(SerializableSupplier<Phase> phase) {
        if (sequence != null) {
            return sequence;
        }
        FutureSupplier<Sequence> ss = new FutureSupplier<>();
        sequence = new SequenceImpl(phase, this.name, id, buildSteps(ss).toArray(new Step[0]));
        ss.set(sequence);
        return sequence;
    }

    void id(int id) {
        this.id = id;
    }

    @Override
    public SequenceBuilder end() {
        return this;
    }

    public ScenarioBuilder endSequence() {
        return scenario;
    }

    public String name() {
        return name;
    }

    public void nextSequence(String nextSequence) {
        this.nextSequence = nextSequence;
    }
}
