<template>
<Card class="error-message">
  <XCircleIcon></XCircleIcon>
  The search could not be completed: {{ message }}
  <Button slim @click="$emit('retry')">Retry</Button>
</Card>
</template>

<script>
import { XCircleIcon } from 'vue-feather-icons';
import Card from '@/components/Card.vue';
import Button from '@/components/Button.vue';

export default {
  name: 'ErrorMessage',
  components: { XCircleIcon, Button, Card },
  props: {
    message: String,
  },
};
</script>

<style lang="scss">
.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 1rem;
  color: var(--base-text-color);
  margin: 0 auto;
  max-width: 50%;
  text-align: left;

  .feather {
    width: 4rem;
    height: 4rem;
    color: $errors;
    margin-bottom: 0.5rem;
  }

  .button {
    margin-top: 1rem;
  }
}
</style>
