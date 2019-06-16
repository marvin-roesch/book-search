<template>
<div :class="{
  'bidirectional-expandable': true,
  'bidirectional-expandable-expanded': expanded,
  'bidirectional-expandable-animated': mayAnimate
}">
  <div class="bidirectional-expandable-start"
       :style="{ marginTop: `${expanded ? 0 : -(startHeight - visibleHeight)}px` }"
       ref="start">
    <slot name="start"></slot>
  </div>
  <slot></slot>
  <div class="bidirectional-expandable-end"
       :style="
       !initialized
       ? undefined
       : { maxHeight: expanded ? `${endHeight}px` : `${visibleHeight}px` }
       "
       ref="end">
    <slot name="end"></slot>
  </div>
</div>
</template>

<script>
export default {
  name: 'BidirectionalExpandable',
  props: {
    expanded: Boolean,
    visibleHeight: Number,
  },
  data() {
    return {
      initialized: false,
      mayAnimate: false,
      startHeight: 900,
      endHeight: this.visibleHeight,
    };
  },
  mounted() {
    const observer = new MutationObserver(() => this.recalculate());
    observer.observe(this.$refs.start, { childList: true });
    observer.observe(this.$refs.end, { childList: true });
    setTimeout(() => this.recalculate(), 100);
    setTimeout(() => {
      this.mayAnimate = true;
    }, 150);
  },
  methods: {
    recalculate() {
      this.startHeight = this.$refs.start.clientHeight;
      this.$refs.end.style.maxHeight = '9999px';
      this.$refs.end.style.height = 'auto';
      this.endHeight = this.$refs.end.clientHeight;
      this.initialized = true;
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
  background: #fff;

  &:before, &:after {
    content: '';
    display: block;
    width: 100%;
    height: 1px;
    position: relative;
    z-index: 1000;
    box-shadow: 0 0 16px 16px #fff;
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

    .bidirectional-expandable-end {
      transition: max-height 0.2s ease-in-out;
    }
  }

  &-expanded {
    &:before, &:after {
      box-shadow: 0 0 0 0 #fff;
    }
  }
}
</style>
