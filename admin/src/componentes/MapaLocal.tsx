import { useEffect } from 'react'
import { CircleMarker, MapContainer, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

interface Props {
  lat?: number
  lng?: number
  aoMudar: (lat: number, lng: number) => void
}

function Clique({ aoMudar }: { aoMudar: Props['aoMudar'] }) {
  useMapEvents({
    click(e) {
      aoMudar(Number(e.latlng.lat.toFixed(7)), Number(e.latlng.lng.toFixed(7)))
    },
  })
  return null
}

function Centralizar({ lat, lng }: { lat?: number; lng?: number }) {
  const mapa = useMap()
  useEffect(() => {
    if (lat != null && lng != null) mapa.setView([lat, lng], Math.max(mapa.getZoom(), 16))
  }, [lat, lng, mapa])
  return null
}

/** Mapa (OpenStreetMap) para marcar onde a barbearia atende. Clique para posicionar. */
export function MapaLocal({ lat, lng, aoMudar }: Props) {
  const temPonto = lat != null && lng != null
  return (
    <div className="mapa">
      <MapContainer
        center={temPonto ? [lat, lng] : [-22.9789, -49.8697]}
        zoom={temPonto ? 16 : 6}
        style={{ height: '100%', width: '100%' }}
        scrollWheelZoom={false}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Clique aoMudar={aoMudar} />
        <Centralizar lat={lat} lng={lng} />
        {temPonto && (
          <CircleMarker center={[lat, lng]} radius={10} pathOptions={{ color: '#ffffff', weight: 3, fillColor: '#2f6b5c', fillOpacity: 1 }} />
        )}
      </MapContainer>
    </div>
  )
}

/** Busca coordenadas pelo endereço (Nominatim / OpenStreetMap). */
export async function geocodificar(endereco: string): Promise<{ lat: number; lng: number } | null> {
  const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=br&q=${encodeURIComponent(endereco)}`
  const r = await fetch(url, { headers: { 'Accept-Language': 'pt-BR' } })
  if (!r.ok) return null
  const lista = (await r.json()) as { lat: string; lon: string }[]
  return lista[0] ? { lat: Number(Number(lista[0].lat).toFixed(7)), lng: Number(Number(lista[0].lon).toFixed(7)) } : null
}

/** Preenche o endereço pelo CEP (ViaCEP). */
export async function buscarCep(cep: string) {
  const limpo = cep.replace(/\D/g, '')
  if (limpo.length !== 8) return null
  const r = await fetch(`https://viacep.com.br/ws/${limpo}/json/`)
  if (!r.ok) return null
  const d = (await r.json()) as { erro?: boolean; logradouro: string; bairro: string; localidade: string; uf: string }
  return d.erro ? null : d
}
