import { useMutation } from '@tanstack/react-query'
import { changeClientPassword } from '../api/clientApi.js'

export default function useChangeClientPassword() {
  return useMutation({ mutationFn: changeClientPassword })
}
