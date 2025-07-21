/**
 * 운영용 스마트 이미지 시스템 - UI Service 폴더 경로 직접 사용
 */
export function useSmartImages() {
    const BASE_IMAGE_PATH = '/images/banners/products/'
    const API_GATEWAY_URL = 'http://13.209.253.241:8080'

    const getProductImage = (product) => {
        // DB의 /upload/product/main/파일명.jpg → UI Service 폴더의 실제 파일명 그대로 사용
        if (product.mainImage && product.mainImage.startsWith('/upload/product/main/')) {
            const fileName = product.mainImage.split('/').pop()
            const finalUrl = `${BASE_IMAGE_PATH}${fileName}`
            return finalUrl
        }

        // 외부 Unsplash 이미지 (https://images.unsplash.com)
        if (product.mainImage && product.mainImage.startsWith('https://images.unsplash.com')) {
            return product.mainImage
        }

        // 다른 외부 URL
        if (product.mainImage && product.mainImage.startsWith('http')) {
            return product.mainImage
        }

        // 기본 이미지
        const defaultImage = `${BASE_IMAGE_PATH}default-product.jpg`
        return defaultImage
    }

    const handleImageError = (event) => {
        // 기본 이미지로 교체
        if (!event.target.src.includes('default-product.jpg')) {
            event.target.src = `${BASE_IMAGE_PATH}default-product.jpg`
        } else {
            // 기본 이미지도 실패하면 숨김
            event.target.style.display = 'none'
        }
    }

    const handleImageLoad = (event) => {
        event.target.style.opacity = '1'
    }

    return {
        getProductImage,
        handleImageError,
        handleImageLoad
    }
}