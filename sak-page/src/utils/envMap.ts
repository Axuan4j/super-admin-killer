import type {EnvConfig} from "@/types/env";

export const env: EnvConfig = {

    API_BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',

    APP_TITLE: import.meta.env.VITE_APP_TITLE || 'SuperKiller Admin'
}