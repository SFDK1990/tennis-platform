/** The side of the square the photo is sent at: some 40 KB as a JPEG (39-fase25.5, decision 3). */
export const PHOTO_SIDE = 400;

/** The largest centred square of an image: where a face usually is. */
export function centerSquare(width: number, height: number): { x: number; y: number; side: number } {
  const side = Math.min(width, height);
  return { x: Math.floor((width - side) / 2), y: Math.floor((height - side) / 2), side };
}

/** Crops the picture to its centred square and draws it at 400 x 400 as a JPEG, before it leaves the phone. */
export async function squareJpeg(file: Blob): Promise<Blob> {
  const image = await createImageBitmap(file);
  const { x, y, side } = centerSquare(image.width, image.height);
  const canvas = document.createElement("canvas");
  canvas.width = PHOTO_SIDE;
  canvas.height = PHOTO_SIDE;
  canvas.getContext("2d")!.drawImage(image, x, y, side, side, 0, 0, PHOTO_SIDE, PHOTO_SIDE);
  image.close();
  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error("No se pudo preparar la foto"))), "image/jpeg", 0.85));
}

/** A photo as a data URL, which the page's CSP allows and which needs no clean-up. */
export function toDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(blob);
  });
}
