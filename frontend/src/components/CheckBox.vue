<template>
<div :class="{
  'checkbox': true,
  'checkbox-with-errors': errored
}">
  <div class="checkbox-content">
    <input
      class="checkbox-input"
      type="checkbox"
      :name="name"
      :value="value"
      @input="onChange"
      v-bind="inputConfig"
      :id="htmlId"
      :checked="value"
    >
    <label :for="htmlId" class="checkbox-label">
      <span class="checkbox-toggle">
        <CheckIcon class="checkbox-toggle-icon"></CheckIcon>
      </span>
      <slot></slot>
    </label>
  </div>
</div>
</template>

<script>
import { CheckIcon } from 'vue-feather-icons';

export default {
  name: 'CheckBox',
  components: { CheckIcon },
  props: {
    name: String,
    value: [Boolean, String],
    hint: String,
    errors: Array,
    inputConfig: Object,
  },
  computed: {
    fieldErrors() {
      return this.errors ? this.errors.filter(e => e.key === this.name) : undefined;
    },
    errored() {
      return this.fieldErrors && this.fieldErrors.length > 0;
    },
    htmlId() {
      return `checkbox-${this.name}`;
    },
  },
  methods: {
    onChange(event) {
      this.$emit('input', event);
    },
  },
};
</script>

<style scoped lang="scss">
$primary: #42B983;
$base-text-color: #2c3e50;
$inactive-color: desaturate(lighten($primary, 20%), 20%);

.checkbox {
  position: relative;

  .checkbox {
    display: flex;
    align-items: stretch;
    flex-direction: row;
    margin-bottom: 0.25rem;
  }

  .checkbox-content {
    display: flex;
  }

  .checkbox-input {
    opacity: 0;
    height: 0;
    margin: 0;
    width: 0;

    & + .checkbox-label:hover {
      cursor: pointer;
    }

    & + .checkbox-label .checkbox-toggle {
      box-sizing: border-box;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      margin-right: 0.5rem;
      background: $inactive-color;
      transition: background-color 0.2s ease-in-out;
      border-radius: 50%;

      &-icon {
        color: white;
        transform: scale(0.75);
        transition: opacity 0.2s ease-in-out;
        opacity: 0;
      }
    }

    &:checked + .checkbox-label .checkbox-toggle {
      background: $primary;

      &-icon {
        opacity: 1;
      }
    }

    &:focus, &:active {
      & + .checkbox-label .checkbox-toggle {
        box-shadow: 0 0 3px rgba(saturate($primary, 30%), 0.75);
      }
    }
  }

  .checkbox-label {
    display: flex;
    flex-direction: row;
    align-items: center;
  }

  &-footer {
    display: flex;
    font-size: 0.9rem;
    margin-bottom: 0.25rem;
    height: 1rem;
  }

  .checkbox-hint {
    color: rgba($base-text-color, 0.5);
  }

  &-with-errors {
    .text-field-input {
      &:hover, &:focus, &:hover ~ .text-field-icon, &:focus ~ .text-field-icon {
        //border-bottom-color: $errors;
      }
    }
  }
}
</style>
