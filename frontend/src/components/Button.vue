<template>
<router-link ref="button" :class="classes" :to="to" v-if="to && !disabled">
  <slot></slot>
</router-link>
<button ref="button" :class="classes" @click.prevent="click" :style="{width: fixedWidth}" v-else>
  <span class="loading-spinner" v-if="loading"></span>
  <slot v-else></slot>
</button>
</template>

<script>
export default {
  name: 'Button',
  props: {
    to: [String, Object],
    slim: Boolean,
    disabled: Boolean,
    loading: {
      type: Boolean,
      default: false,
    },
  },
  watch: {
    loading(loadingNow) {
      if (loadingNow) {
        this.fixedWidth = `${this.$refs.button.getBoundingClientRect().width}px`;
      } else {
        this.fixedWidth = 'auto';
      }
    },
  },
  data() {
    return {
      fixedWidth: 'auto',
    };
  },
  computed: {
    classes() {
      return {
        button: true,
        'button-slim': this.slim,
        'button-loading': this.loading,
        'button-disabled': this.disabled,
      };
    },
  },
  methods: {
    click() {
      if (!this.disabled) {
        this.$emit('click');
      }
    },
  },
};
</script>

<style scoped lang="scss">
.button {
  background: $primary;
  font-weight: bold;
  padding: 1rem;
  border: none;
  color: #edf5fa;
  text-align: center;
  font-size: 1rem;
  box-shadow: 0 0 0 rgba(0, 0, 0, 0);
  transition: box-shadow 0.1s ease-in-out;
  border-radius: 2px;
  text-decoration: none;
  display: inline-block;
  box-sizing: border-box;
  height: 3.125rem;

  &:hover {
    cursor: pointer;
    box-shadow: 0 0.25rem 0.5rem rgba(0, 0, 0, 0.2);
  }

  &:hover, &:active, &:focus {
    color: #edf5fa;
    background: saturate($primary, 20%);
    outline: none;
  }

  &-disabled {
    background: desaturate($primary, 20%);

    &:hover, &:active, &:focus {
      cursor: initial;
      box-shadow: none;
      background: desaturate($primary, 20%);
    }
  }

  &-slim {
    padding: 0.75rem 1rem;
    height: 2.5rem;
  }

  &-loading {
    line-height: 1rem;
    padding: 0.75rem 1rem;
  }

  &-loading.button-slim {
    padding: 0.5rem 1rem;
  }

  .loading-spinner {
    box-sizing: border-box;
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 50%;
    display: inline-block;
    border: 0.25rem solid rgba(255, 255, 255, 0.3);
    border-top-color: rgba(255, 255, 255, 0.75);
    animation: 1.5s spin infinite linear;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
