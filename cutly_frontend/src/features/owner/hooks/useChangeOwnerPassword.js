import { useMutation } from '@tanstack/react-query'
import { changeOwnerPassword } from '../api/ownerSecurityApi.js'

export default function useChangeOwnerPassword() {
  return useMutation({ mutationFn: changeOwnerPassword })
}
