import uuid from 'uuid/v4';

const initialState = {
  activeNotifications: [],
};

const getters = {};

const actions = {
  push({ commit }, notification) {
    commit('push', notification);
    setTimeout(() => {
      commit('pop');
    }, 2000);
  },
};

const mutations = {
  push(state, notification) {
    state.activeNotifications.push({ ...notification, id: uuid() });
  },
  pop(state) {
    state.activeNotifications.shift();
  },
};

export default {
  namespaced: true,
  state: initialState,
  getters,
  actions,
  mutations,
};
