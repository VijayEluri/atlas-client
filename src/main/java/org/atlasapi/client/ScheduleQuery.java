package org.atlasapi.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.atlasapi.media.entity.Channel;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.output.Annotation;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.metabroadcast.common.url.QueryStringParameters;

public final class ScheduleQuery {

    private final Joiner CSV = Joiner.on(',');

    private final List<String> channels;
    private final List<String> channelIds;
    private final Interval interval;
    private final DateTime start;
    private final Integer count;
    private final List<String> publishers;
    private ImmutableSet<Annotation> annotations;

    public ScheduleQuery(ScheduleQueryBuilder builder) {
        checkArgument(!builder.channels.isEmpty() || !builder.channelIds.isEmpty(), "No channels specified");
        checkArgument(builder.channels.isEmpty() || builder.channelIds.isEmpty(), "cannot specify both channels and channelIds");

        channels = ImmutableList.copyOf(builder.channels);
        channelIds = ImmutableList.copyOf(builder.channelIds);

        checkArgument( builder.interval != null 
                || (builder.start != null && builder.count != null), 
                "No schedule period specified");

        interval = builder.interval;
        start = builder.start;
        count = builder.count;

        publishers = ImmutableList.copyOf(builder.publishers);
        annotations = builder.annotations;
    }

    QueryStringParameters toParams() {
        QueryStringParameters params = new QueryStringParameters();
        if (!channels.isEmpty()) {
            params.add("channel", CSV.join(channels));
        }
        if (!channelIds.isEmpty()) {
            params.add("channel_id", CSV.join(channelIds));
        }

        if (interval != null) {
            params.add("from", interval.getStart().toString());
            params.add("to", interval.getEnd().toString());
        } else {
            params.add("from", start.toString());
            params.add("count", String.valueOf(count));
        }

        if (!publishers.isEmpty()) {
            params.add("publisher", CSV.join(publishers));
        }
        if (!annotations.isEmpty()) {
            params.add("annotations", CSV.join(Iterables.transform(annotations, Annotation.TO_KEY)));
        }

        return params;
    }

    public static final class ScheduleQueryBuilder {		
        private LinkedHashSet<String> channels = Sets.newLinkedHashSet();
        private LinkedHashSet<String> channelIds = Sets.newLinkedHashSet();
        private LinkedHashSet<String> publishers = Sets.newLinkedHashSet();
        private ImmutableSet<Annotation> annotations = ImmutableSet.of();

        private Interval interval = null;
        private DateTime start = null;
        private Integer count = null;

        private ScheduleQueryBuilder() {}

        @Deprecated
        public ScheduleQueryBuilder withChannels(Channel... channels) {
            return withChannels(Arrays.asList(channels));
        }

        @Deprecated
        public ScheduleQueryBuilder withChannels(Iterable<Channel> channels) {
            this.channels.addAll(ImmutableList.copyOf(Iterables.transform(channels, new Function<Channel, String>() {
                @Override
                public String apply(Channel input) {
                    return input.key();
                }
            })));
            return this;
        }		

        public ScheduleQueryBuilder withChannels(String... channels) {
            this.channels.addAll(ImmutableList.copyOf(channels));
            return this;
        }       

        public ScheduleQueryBuilder withChannelIds(String... channelIds) {
            return withChannelIds(ImmutableList.copyOf(channelIds));
        }

        public ScheduleQueryBuilder withChannelIds(Iterable<String> channelIds) {
            this.channelIds.addAll(ImmutableList.copyOf(channelIds));
            return this;
        }

        public ScheduleQueryBuilder withOnBetween(Interval interval) {
            checkState(start == null, "Cannot specify both an interval and number of items");
            this.interval = interval;
            return this;
        }

        public ScheduleQueryBuilder withStartTimeAndNumberOfItems(DateTime start, int count) {
            checkState(interval == null, "Cannot specify both an interval and number of items");
            this.start = start;
            this.count  = count;
            return this;
        }

        @Deprecated
        public ScheduleQueryBuilder withPublishers(Publisher... publishers) {
            return withPublishers(Arrays.asList(publishers));
        }

        @Deprecated
        public ScheduleQueryBuilder withPublishers(Iterable<Publisher> publishers) {
            this.publishers.addAll(ImmutableList.copyOf(Iterables.transform(publishers, new Function<Publisher, String>() {
                @Override
                public String apply(Publisher input) {
                    return input.key();
                }
            })));
            return this;
        }

        public ScheduleQueryBuilder withPublishers(String... publishers) {
            this.publishers.addAll(ImmutableList.copyOf(publishers));
            return this;
        }

        public ScheduleQueryBuilder withAnnotations(Iterable<Annotation> annotations) {
            this.annotations = ImmutableSet.copyOf(annotations);
            return this;
        }

        public ScheduleQueryBuilder withAnnotations(Annotation...annotations) {
            this.annotations = ImmutableSet.copyOf(annotations);
            return this;
        }

        public ScheduleQuery build() {
            return new ScheduleQuery(this);
        }
    }

    public static ScheduleQueryBuilder builder() {
        return new ScheduleQueryBuilder();
    }
}
