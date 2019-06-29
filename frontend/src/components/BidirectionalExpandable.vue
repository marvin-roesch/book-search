<template>
<div :class="{
  'bidirectional-expandable': true,
  'bidirectional-expandable-expanded': expanded,
  'bidirectional-expandable-animated': mayAnimate
}" ref="expandable">
  <div class="bidirectional-expandable-start"
       :style="{ marginTop: `${expanded ? 0 : -(startHeight - visibleHeight)}px` }"
       ref="start">
    <slot name="start"></slot>
  </div>
  <slot></slot>
  <div class="bidirectional-expandable-end-container"
       :style="{ maxHeight: expanded ? `${endHeight}px` : `${visibleHeight}px` }">
    <div class="bidirectional-expandable-end" ref="end">
      <slot name="end"></slot>
    </div>
  </div>
</div>
</template>

<script>
import ResizeObserverLite from 'resize-observer-lite';

export default {
  name: 'BidirectionalExpandable',
  props: {
    expanded: Boolean,
    visibleHeight: Number,
  },
  data() {
    return {
      mayAnimate: false,
      startHeight: 900,
      endHeight: this.visibleHeight,
    };
  },
  mounted() {
    const startObserver = new ResizeObserverLite(size => this.recalculateStart(size));
    const endObserver = new ResizeObserverLite(size => this.recalculateEnd(size));
    startObserver.observe(this.$refs.start);
    endObserver.observe(this.$refs.end);
    setTimeout(() => {
      this.mayAnimate = true;
    }, 150);
  },
  methods: {
    recalculateStart(size) {
      this.mayAnimate = false;
      this.startHeight = size.height;
      setTimeout(() => {
        this.mayAnimate = true;
      }, 150);
    },
    recalculateEnd(size) {
      this.mayAnimate = false;
      this.endHeight = size.height;
      setTimeout(() => {
        this.mayAnimate = true;
      }, 150);
    },
  },
};
</script>

<style lang="scss">
.bidirectional-expandable {
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
  min-height: 5rem;
  background: var(--section-bg);

  &:before, &:after {
    content: '';
    display: block;
    width: 100%;
    height: 1px;
    position: relative;
    z-index: 1000;
    box-shadow: 0 0 16px 16px var(--section-bg);
    transition: box-shadow 0.2s ease-in-out;
  }

  &:before {
    top: -1px;
  }

  &:after {
    bottom: 0;
  }

  &-loader {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
  }

  &-start {
    overflow: auto;
  }

  &-end {
    overflow-y: hidden;
  }

  &-animated {
    .bidirectional-expandable-start {
      transition: margin-top 0.2s ease-in-out;
    }

    .bidirectional-expandable-end-container {
      transition: max-height 0.2s ease-in-out;
    }
  }

  &-expanded {
    &:before, &:after {
      box-shadow: 0 0 0 0 var(--section-bg);
    }
  }
}
</style>
