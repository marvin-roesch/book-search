<template>
<div :class="{notification: true, [typeClass]: true}">
  <span class="notification-icon">
    <CheckIcon v-if="type === 'success'"></CheckIcon>
    <XCircleIcon v-else-if="type === 'error'"></XCircleIcon>
  </span>
  <span class="notification-message">
    <slot></slot>
  </span>
</div>
</template>

<script>
import { CheckIcon, XCircleIcon } from 'vue-feather-icons';

export default {
  name: 'Notification',
  components: { CheckIcon, XCircleIcon },
  props: {
    type: String,
  },
  computed: {
    typeClass() {
      return `notification-${this.type}`;
    },
  },
};
</script>

<style scoped lang="scss">
.notification {
  display: flex;
  padding: 2rem;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.2);
  border-top: none;
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
  flex-direction: row;
  align-items: center;
  transition: all .2s ease-in-out;

  &-icon {
    display: flex;
    transform: scale(2);
    line-height: 1;
  }

  &-message {
    padding-left: 1rem;
  }

  &-success .notification-icon {
    color: #67b83e;
  }

  &-error .notification-icon {
    color: $errors;
  }

  &:last-child {
    border-radius: 0 0 3px 3px;
  }
}
</style>
