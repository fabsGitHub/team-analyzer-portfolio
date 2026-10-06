<template>
    <div v-if="open" role="dialog">
        <button @click="$emit('close')">Cancel</button>
        <button @click="$emit('confirm')">Confirm</button>
        <slot name="title" />
        <slot />
    </div>
</template>

<script setup lang="ts">
defineProps<{ open: boolean }>()
defineEmits<{ (e: 'close'): void; (e: 'confirm'): void }>()
</script>
