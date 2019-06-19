<template>
<div :class="{
  'text-field': true,
  'text-field-with-icon': $slots.icon,
  'text-field-with-errors': errored,
  'text-field-non-empty': value && value.length > 0,
  'text-field-disabled': disabled
}">
  <div class="text-field-content">
    <input
      class="text-field-input"
      :type="type"
      :name="name"
      :placeholder="placeholder"
      :value="value"
      @input="$emit('input', $event.target.value)"
      :disabled="disabled"
      v-bind="inputConfig"
      :id="`textfield-${name}`"
    >
    <label class="text-field-icon" :for="`textfield-${name}`" v-if="$slots.icon">
      <slot name="icon"></slot>
    </label>
  </div>
</div>
</template>

<script>
export default {
  name: 'TextField',
  props: {
    type: {
      type: String,
      default: 'text',
    },
    name: String,
    value: String,
    placeholder: String,
    hint: String,
    errors: Array,
    disabled: Boolean,
    inputConfig: Object,
  },
  computed: {
    fieldErrors() {
      return this.errors ? this.errors.filter(e => e.key === this.name) : undefined;
    },
    errored() {
      return this.fieldErrors && this.fieldErrors.length > 0;
    },
  },
};
</script>

<style scoped lang="scss">
$inactive-color: desaturate(lighten($primary, 20%), 20%);

.text-field {
  position: relative;
  transition: opacity 0.2s ease-in-out;

  .text-field-content {
    display: flex;
    align-items: stretch;
    flex-direction: row-reverse;
    margin-bottom: 0.25rem;
  }

  .text-field-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    color: lighten($inactive-color, 10%);
    border-bottom: 2px solid lighten($inactive-color, 20%);
    transition: border-bottom-color 0.2s ease-in-out, color 0.2s ease-in-out;
  }

  .text-field-input {
    box-sizing: border-box;
    flex-grow: 1;
    border: none;
    padding: 0.75rem;
    background: none;
    border-bottom: 2px solid lighten($inactive-color, 20%);
    transition: border-bottom-color 0.2s ease-in-out;
    font-size: 1rem;
    color: inherit;

    &:hover, &:focus, &:hover ~ .text-field-icon, &:focus ~ .text-field-icon {
      border-bottom-color: $primary;
      outline: none;
    }

    &::placeholder {
      color: $inactive-color;
    }
  }

  &-non-empty {
    .text-field-icon {
      color: $primary;
    }
  }

  &-with-icon {
    .text-field-input {
      padding-left: 0.5rem;
    }
  }

  &-footer {
    display: flex;
    font-size: 0.9rem;
    margin-bottom: 0.25rem;
    height: 1rem;
  }

  .text-field-hint {
    color: rgba($base-text-color, 0.5);
  }

  &-with-errors {
    .text-field-icon, .text-field-input {
      border-bottom-color: $errors;
    }

    .text-field-icon, .text-field-message {
      color: $errors;
    }

    .text-field-input {
      &:hover, &:focus, &:hover ~ .text-field-icon, &:focus ~ .text-field-icon {
        border-bottom-color: $errors;
      }
    }
  }

  &.text-field-disabled {
    opacity: 0.5;

    .text-field-input {
      &:hover, &:focus, &:hover ~ .text-field-icon, &:focus ~ .text-field-icon {
        border-bottom-color: lighten($inactive-color, 20%);
      }
    }
  }
}
</style>
