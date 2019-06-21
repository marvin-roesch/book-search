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
      <slot></slot>
    </label>
  </div>
</div>
</template>

<script>
export default {
  name: 'CheckBox',
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
.checkbox {
  position: relative;

  .checkbox-content {
    display: flex;
    align-items: center;
  }

  .checkbox-input {
    & + .checkbox-label:hover {
      cursor: pointer;
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
