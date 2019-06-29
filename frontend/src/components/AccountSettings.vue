<template>
<Card class="account-settings" title="Account Settings">
  <div class="account-settings-section">
    <h3>Appearance</h3>
    <CheckBox :value="darkMode" @input="$store.commit('setDarkMode', $event.target.checked)">
      Enable Dark Mode (Currently only affects this device)
    </CheckBox>
  </div>
  <form class="account-settings-section">
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
    <div class="card-footer">
      <a href="#" @click.prevent="$router.back()">Cancel</a>
      <Button slim @click="change" :loading="verifying">Change</Button>
    </div>
  </form>
</Card>
</template>

<script>
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';
import { KeyIcon, LockIcon, RepeatIcon } from 'vue-feather-icons';
import Card from '@/components/Card.vue';
import CheckBox from '@/components/CheckBox.vue';
import { mapState } from 'vuex';

export default {
  name: 'AccountSettings',
  components: {
    CheckBox,
    Card,
    Button,
    TextField,
    LockIcon,
    KeyIcon,
    RepeatIcon,
  },
  data() {
    return {
      oldPassword: '',
      newPassword: '',
      newPasswordRepeat: '',
      verifying: false,
    };
  },
  computed: mapState(['darkMode']),
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
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
      this.verifying = false;
    },
  },
};
</script>

<style lang="scss">
.account-settings {
  margin: 0 auto;
  width: 50vw;
  max-width: $max-content-width;
  box-sizing: border-box;

  form {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    .checkbox {
      display: inline-block;
      margin-top: 0.5rem;
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }
  }

  &-section {
    h3 {
      padding: 0;
      margin: 0;
    }

    margin-bottom: 0.5rem;

    &:last-child {
      margin-bottom: 0;
    }
  }

  @media (max-width: 640px) {
    width: 100%;
  }
}
</style>
