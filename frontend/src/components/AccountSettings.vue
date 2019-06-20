<template>
<Card class="account-settings" title="Account Settings">
  <form>
    <h3>Change password</h3>
    <TextField
      type="password"
      name="old-password"
      v-model="oldPassword"
      placeholder="Current password"
    >
      <template slot="icon">
      <LockIcon></LockIcon>
      </template>
    </TextField>
    <TextField
      type="password"
      name="new-password"
      v-model="newPassword"
      placeholder="New password"
    >
      <template slot="icon">
      <KeyIcon></KeyIcon>
      </template>
    </TextField>
    <TextField
      type="password"
      name="repeat-password"
      v-model="newPasswordRepeat"
      placeholder="Repeat password"
    >
      <template slot="icon">
      <RepeatIcon></RepeatIcon>
      </template>
    </TextField>
    <Button slim @click="change" :loading="verifying">Change</Button>
  </form>
</Card>
</template>

<script>
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';
import { KeyIcon, LockIcon, RepeatIcon } from 'vue-feather-icons';
import Card from '@/components/Card.vue';

export default {
  name: 'AccountSettings',
  components: { Card, Button, TextField, LockIcon, KeyIcon, RepeatIcon },
  data() {
    return {
      oldPassword: '',
      newPassword: '',
      newPasswordRepeat: '',
      verifying: false,
    };
  },
  methods: {
    async change() {
      const {
        oldPassword, newPassword, newPasswordRepeat,
      } = this;
      this.verifying = true;
      try {
        const { data: { message } } = await this.$api.patch(
          '/auth/password',
          { oldPassword, newPassword, newPasswordRepeat },
        );
        this.$router.replace(this.$route.query.redirect || '/');
      } catch (error) {
      }
      this.verifying = false;
    },
  },
};
</script>

<style lang="scss">
.account-settings {
  margin: 0 auto;

  form {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    h3 {
      padding: 0;
      margin: 0;
    }

    .checkbox {
      display: inline-block;
      margin-top: 0.5rem;
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }

    .button {
      margin-top: 0.5rem;
      align-self: flex-end;
    }
  }
}
</style>
